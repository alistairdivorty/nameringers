package com.nameringers;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.DockerImage;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.deployment.*;
import software.amazon.awscdk.services.s3.assets.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.emrserverless.*;
import software.amazon.awscdk.services.emrserverless.CfnApplication.*;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class EMRServerlessStack extends Stack {
    public EMRServerlessStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public EMRServerlessStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        IVpc vpc = Vpc.fromLookup(this, "Vpc",
                VpcLookupOptions.builder().vpcName("VPC").build());

        Bucket bucket = Bucket.Builder.create(this, "Bucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        List<String> packagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "sbt assembly " +
                        "&& cp /asset-input/target/scala-2.12/spark-assembly-0.1.0.jar /asset-output/");

        BundlingOptions bundlingOptions = BundlingOptions.builder()
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

        BucketDeployment bucketDeployment = BucketDeployment.Builder.create(this, "ArtifactsDeployment")
                .sources(
                        List.of(
                                Source.asset("../spark", AssetOptions.builder()
                                        .bundling(bundlingOptions)
                                        .build())))
                .destinationBucket(bucket)
                .extract(false)
                .prune(true)
                .destinationKeyPrefix("artifacts")
                .memoryLimit(500)
                .ephemeralStorageSize(Size.mebibytes(1000))
                .build();

        PolicyDocument s3Policy = PolicyDocument.Builder.create()
                .statements(List.of(PolicyStatement.Builder.create()
                        .actions(List.of("s3:*"))
                        .resources(List.of("*"))
                        .build()))
                .build();

        PolicyDocument ecrPolicy = PolicyDocument.Builder.create()
                .statements(List.of(PolicyStatement.Builder.create()
                        .actions(List.of("ecr:*"))
                        .resources(List.of("*"))
                        .build()))
                .build();

        Role emrServerlessRole = Role.Builder.create(this, "EMRServerlessRole")
                .assumedBy(new ServicePrincipal("emr-serverless.amazonaws.com"))
                .inlinePolicies(Map.of("s3Policy", s3Policy, "ecrPolicy", ecrPolicy))
                .build();

        emrServerlessRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSLambda_FullAccess"));

        SecurityGroup defaultSecurityGroup = SecurityGroup.Builder.create(this,
                "DefaultSecurityGroup").vpc(vpc)
                .build();

        CfnApplication emrServerlessApp = CfnApplication.Builder.create(this,
                "EMRServerlessApp")
                .releaseLabel("emr-6.9.0")
                .type("SPARK")
                .autoStartConfiguration(AutoStartConfigurationProperty.builder()
                        .enabled(true)
                        .build())
                .autoStopConfiguration(AutoStopConfigurationProperty.builder()
                        .enabled(true)
                        .idleTimeoutMinutes(1)
                        .build())
                .name("nameringers")
                .networkConfiguration(NetworkConfigurationProperty.builder()
                        .securityGroupIds(List.of(defaultSecurityGroup.getSecurityGroupId()))
                        .subnetIds(vpc.selectSubnets().getSubnetIds())
                        .build())
                .build();

        CfnOutput.Builder.create(this, "RoleARN")
                .value(emrServerlessRole.getRoleArn())
                .exportName("NameRingersEMRServerlessRoleARN")
                .build();

        CfnOutput.Builder.create(this, "ApplicationID")
                .value(emrServerlessApp.getAttrApplicationId())
                .exportName("NameRingersEMRServerlessApplicationID")
                .build();

        CfnOutput.Builder.create(this, "BucketName")
                .value(bucket.getBucketName())
                .exportName("NameRingersEMRServerlessApplicationBucketName")
                .build();

        CfnOutput.Builder.create(this, "ModelsURI")
                .value(bucket.s3UrlForObject("models/"))
                .exportName("NameRingersEMRServerlessApplicationModelsURI")
                .build();

        CfnOutput.Builder.create(this, "AssemblyJarURI")
                .value(bucket.s3UrlForObject("artifacts/" + Fn.select(0, bucketDeployment.getObjectKeys())))
                .exportName("NameRingersEMRServerlessAssemblyJarURI")
                .build();

        CfnOutput.Builder.create(this, "LogsURI")
                .value(bucket.s3UrlForObject("logs/"))
                .exportName("NameRingersEMRServerlessApplicationLogsURI")
                .build();
    }
}
