package com.nameringers;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.efs.*;

public class WeaviateFileSystemStack extends Stack {
    public WeaviateFileSystemStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public WeaviateFileSystemStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        IVpc vpc = Vpc.fromLookup(this, "Vpc", VpcLookupOptions.builder().vpcName("VPC").build());

        SecurityGroup defaultSecurityGroup = SecurityGroup.Builder.create(this, "DefaultSecurityGroup").vpc(vpc)
                .build();

        CfnOutput.Builder.create(this, "DefaultSecurityGroupID")
                .value(defaultSecurityGroup.getSecurityGroupId())
                .exportName("WeaviateFileSystemSecurityGroupID")
                .build();

        FileSystem fileSystem = FileSystem.Builder.create(this, "FileSystem")
                .fileSystemName("weaviate")
                .vpc(vpc)
                .securityGroup(defaultSecurityGroup)
                .removalPolicy(RemovalPolicy.RETAIN)
                .throughputMode(ThroughputMode.ELASTIC)
                .build();

        CfnOutput.Builder.create(this, "FileSystemARN")
                .value(fileSystem.getFileSystemArn())
                .exportName("WeaviateFileSystemARN")
                .build();

        AccessPoint accessPoint = fileSystem.addAccessPoint("DataAccessPoint",
                AccessPointOptions.builder()
                        .createAcl(
                                Acl
                                        .builder()
                                        .ownerGid("1001").ownerUid("1001")
                                        .permissions("750")
                                        .build())
                        .path("/data")
                        .posixUser(
                                PosixUser
                                        .builder()
                                        .gid("1001").uid("1001")
                                        .build())
                        .build());

        CfnOutput.Builder.create(this, "AccessPoint")
                .value(accessPoint.getAccessPointArn())
                .exportName("WeaviateFileSystemDataAccessPointARN")
                .build();
    }
}
