package com.nameringers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.efs.FileSystem;
import software.amazon.awscdk.services.efs.AccessPoint;
import software.amazon.awscdk.services.efs.AccessPointOptions;
import software.amazon.awscdk.services.efs.Acl;
import software.amazon.awscdk.services.efs.PosixUser;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.iam.PolicyStatement;

import io.github.cdimascio.dotenv.Dotenv;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class ZoneFileStack extends Stack {
    public ZoneFileStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ZoneFileStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Dotenv dotenv = Dotenv.load();

        Bucket bucket = Bucket.Builder.create(this, "Bucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        IVpc vpc = Vpc.fromLookup(this, "Vpc", VpcLookupOptions.builder().vpcName("VPC").build());

        FileSystem fileSystem = FileSystem.Builder.create(this, "FileSystem")
                .vpc(vpc)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        AccessPoint accessPoint = fileSystem.addAccessPoint("AccessPoint",
                AccessPointOptions.builder()
                        .createAcl(
                                Acl
                                        .builder()
                                        .ownerGid("1001").ownerUid("1001").permissions("750")
                                        .build())
                        .path("/export/lambda")
                        .posixUser(
                                PosixUser
                                        .builder()
                                        .gid("1001").uid("1001")
                                        .build())
                        .build());

        List<String> packagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "mvn clean install " +
                        "&& cp /asset-input/target/zonefile-client.jar /asset-output/");

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(packagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the
                        // container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()))
                .user("root")
                .outputType(ARCHIVED);

        Function function = Function.Builder.create(this, "Function")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../zonefile-client/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(packagingInstructions)
                                .build())
                        .build()))
                .handler("com.nameringers.zonefile.App")
                .memorySize(5120)
                .timeout(Duration.minutes(10))
                .logRetention(RetentionDays.ONE_DAY)
                .vpc(vpc)
                .filesystem(
                        software.amazon.awscdk.services.lambda.FileSystem.fromEfsAccessPoint(accessPoint,
                                "/mnt/zonefiles"))
                .environment(Map.of("API_TOKEN", dotenv.get("API_TOKEN"), "BUCKET", bucket.getBucketName()))
                .build();

        PolicyStatement s3Policy = PolicyStatement.Builder.create()
                .actions(List.of("s3:ListBuckets", "s3:PutObject"))
                .resources(List.of(bucket.getBucketArn(), bucket.arnForObjects("*"))).build();

        function.addToRolePolicy(s3Policy);

        CfnOutput.Builder.create(this, "ZoneFilesURI")
                .value(bucket.s3UrlForObject())
                .exportName("ZoneFilesURI")
                .build();
    }
}
