package com.nameringers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.DockerImage;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;

import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Version;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Tracing;

import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ec2.IVpc;

import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.Cors;
import software.amazon.awscdk.services.apigateway.DomainNameOptions;

import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.targets.ApiGateway;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.IAliasRecordTarget;

import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;

import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;

import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.iam.PolicyStatement;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import io.github.cdimascio.dotenv.Dotenv;

public class WeaviateClientStack extends Stack {
    public WeaviateClientStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public WeaviateClientStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Dotenv dotenv = Dotenv.load();

        IVpc vpc = Vpc.fromLookup(this, "Vpc", VpcLookupOptions.builder().vpcName("VPC").build());

        IBucket bucket = Bucket.fromBucketName(this, "Bucket",
                Fn.importValue("NameRingersEMRServerlessApplicationBucketName"));

        LayerVersion layerVersion = LayerVersion.Builder.create(this, "LamdaLayer")
                .removalPolicy(RemovalPolicy.DESTROY)
                .code(Code.fromBucket(bucket, "models/tfidf/bundle.zip"))
                .compatibleArchitectures(List.of(Architecture.X86_64, Architecture.ARM_64))
                .build();

        List<String> packagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "sbt assembly " +
                        "&& cp /asset-input/target/scala-2.12/weaviate-client-assembly-0.1.0.jar /asset-output/");

        BundlingOptions builderOptions = BundlingOptions.builder()
                .command(packagingInstructions)
                .image(DockerImage.fromRegistry("mozilla/sbt"))
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the
                        // container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()))
                .user("root")
                .outputType(ARCHIVED)
                .build();

        Function function = Function.Builder.create(this, "Function")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../weaviate-client/", AssetOptions.builder()
                        .bundling(builderOptions)
                        .build()))
                .handler("client.ScalaHandler::handleRequest")
                .layers(List.of(layerVersion))
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .logRetention(RetentionDays.ONE_DAY)
                .vpc(vpc)
                .environment(Map.of("WEAVIATE_ENDPOINT",
                        "http://" + Fn.importValue("WeaviateLoadBalancerDNSName") + "/v1/graphql"))
                // .tracing(Tracing.ACTIVE)
                .build();

        CfnFunction functionResource = (CfnFunction) function.getNode().getDefaultChild();
        functionResource.addPropertyOverride("SnapStart", Map.of("ApplyOn",
                "PublishedVersions"));

        Rule eventRule = Rule.Builder.create(this, "Rule")
                .schedule(Schedule.rate(Duration.minutes(5)))
                .build();

        eventRule.addTarget(new LambdaFunction(function));

        LambdaRestApi api = LambdaRestApi.Builder.create(this, "RestAPI")
                .handler(function)
                .build();

        IHostedZone hostedZone = HostedZone.fromHostedZoneAttributes(this, "HostedZone", HostedZoneAttributes.builder()
                .hostedZoneId(dotenv.get("HOSTED_ZONE_ID"))
                .zoneName("nameringers.com")
                .build());

        api.addDomainName("CustomDomain", DomainNameOptions.builder()
                .domainName("api.nameringers.com")
                .certificate(Certificate.Builder.create(this, "Certificate").domainName("api.nameringers.com")
                        .validation(CertificateValidation.fromDns(hostedZone)).build())
                .build());

        ARecord.Builder.create(this, "AliasRecord")
                .zone(hostedZone)
                .recordName("api.nameringers.com")
                .target(RecordTarget.fromAlias(new ApiGateway(api)))
                .build();
    }
}
