package com.nameringers;

import java.util.Map;

import software.constructs.Construct;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Duration;

import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupImportOptions;
import software.amazon.awscdk.services.ec2.InstanceType;

import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedEc2Service;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.AmiHardwareType;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.EcsOptimizedImage;
import software.amazon.awscdk.services.ecs.EcsOptimizedImageOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.ecs.EfsVolumeConfiguration;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.MountPoint;
import software.amazon.awscdk.services.ecs.AddCapacityOptions;
import software.amazon.awscdk.services.ecs.AuthorizationConfig;

import software.amazon.awscdk.services.efs.FileSystem;
import software.amazon.awscdk.services.efs.IFileSystem;
import software.amazon.awscdk.services.efs.FileSystemAttributes;
import software.amazon.awscdk.services.efs.AccessPoint;
import software.amazon.awscdk.services.efs.IAccessPoint;
import software.amazon.awscdk.services.efs.AccessPointAttributes;
import software.amazon.awscdk.services.efs.Acl;
import software.amazon.awscdk.services.efs.PosixUser;
import software.amazon.awscdk.services.efs.ThroughputMode;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecr.assets.Platform;

import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.logs.RetentionDays;

public class WeaviateStack extends Stack {
    public WeaviateStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public WeaviateStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        IVpc vpc = Vpc.fromLookup(this, "Vpc", VpcLookupOptions.builder().vpcName("VPC").build());

        Cluster cluster = Cluster.Builder.create(this, "Cluster").vpc(vpc).capacity(AddCapacityOptions.builder()
                .instanceType(new InstanceType("t4g.large"))
                .machineImage(EcsOptimizedImage.amazonLinux2(AmiHardwareType.ARM,
                        EcsOptimizedImageOptions.builder()
                                .cachedInContext(true)
                                .build()))
                .desiredCapacity(1)
                .build()).build();

        ApplicationLoadBalancedEc2Service ec2Service = ApplicationLoadBalancedEc2Service.Builder
                .create(this, "Ec2Service")
                .cluster(cluster)
                .memoryReservationMiB(7000)
                .desiredCount(1)
                .minHealthyPercent(0)
                .maxHealthyPercent(100)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("weaviate")
                                .image(ContainerImage.fromDockerImageAsset(
                                        DockerImageAsset.Builder.create(this, "ImageAsset")
                                                .directory(System.getProperty("user.dir"))
                                                .file("weaviate.Dockerfile")
                                                .platform(Platform.LINUX_ARM64)
                                                .build()))
                                .environment(Map.of(
                                        "QUERY_DEFAULTS_LIMIT", "25",
                                        "AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED", "true",
                                        "PERSISTENCE_DATA_PATH", "/mnt/weaviate",
                                        "DEFAULT_VECTORIZER_MODULE", "none",
                                        "ENABLE_MODULES", "",
                                        "CLUSTER_HOSTNAME", "node1"))
                                .logDriver(LogDrivers
                                        .awsLogs(AwsLogDriverProps.builder().streamPrefix("WeaviateContainer")
                                                .logRetention(RetentionDays.ONE_DAY).build()))
                                .build())
                .publicLoadBalancer(true)
                .build();

        ec2Service.getTargetGroup()
                .configureHealthCheck(
                        HealthCheck.builder().path("/v1/.well-known/live").interval(Duration.seconds(120))
                                .timeout(Duration.seconds(30)).build());

        ec2Service.getTargetGroup().setAttribute("deregistration_delay.timeout_seconds", "0");

        ec2Service.getTaskDefinition().getDefaultContainer().addPortMappings(PortMapping.builder()
                .containerPort(8080)
                .protocol(Protocol.TCP)
                .build());

        ec2Service.getTaskDefinition().getTaskRole()
                .addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("CloudWatchLogsFullAccess"));

        Port allowedPorts = Port.Builder.create().protocol(software.amazon.awscdk.services.ec2.Protocol.TCP).fromPort(0)
                .toPort(65535).stringRepresentation("All").build();

        ec2Service.getService().getConnections().allowFromAnyIpv4(allowedPorts);

        IFileSystem fileSystem = FileSystem.fromFileSystemAttributes(this, "FileSystem", FileSystemAttributes.builder()
                .fileSystemArn(Fn.importValue("WeaviateFileSystemARN"))
                .securityGroup(
                        SecurityGroup.fromSecurityGroupId(this, "SG",
                                Fn.importValue("WeaviateFileSystemSecurityGroupID"),
                                SecurityGroupImportOptions.builder()
                                        .allowAllOutbound(false)
                                        .build()))
                .build());

        fileSystem.getConnections().allowInternally(Port.tcp(2049));

        fileSystem.getConnections().allowDefaultPortFrom(ec2Service.getService());

        ec2Service.getTaskDefinition().getTaskRole().addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonElasticFileSystemClientReadWriteAccess"));

        IAccessPoint accessPoint = AccessPoint.fromAccessPointAttributes(this, "AccessPoint",
                AccessPointAttributes.builder()
                        .accessPointArn(Fn.importValue("WeaviateFileSystemDataAccessPointARN")).build());

        Volume volume = Volume.builder().name("weaviate")
                .efsVolumeConfiguration(EfsVolumeConfiguration.builder()
                        .fileSystemId(fileSystem.getFileSystemId())
                        .transitEncryption("ENABLED")
                        .authorizationConfig(AuthorizationConfig.builder()
                                .accessPointId(accessPoint.getAccessPointId())
                                .iam("DISABLED")
                                .build())
                        .build())
                .build();

        ec2Service.getTaskDefinition().addVolume(volume);

        ec2Service.getTaskDefinition().getDefaultContainer().addMountPoints(MountPoint.builder()
                .containerPath("/mnt/weaviate")
                .readOnly(false)
                .sourceVolume("weaviate")
                .build());

        CfnOutput.Builder.create(this, "LoadBalancerDNSName")
                .value(ec2Service.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("WeaviateLoadBalancerDNSName")
                .build();
    }
}
