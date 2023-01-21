package com.nameringers;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        new ZoneFileStack(app, "ZoneFileStack", StackProps.builder()
                .env(env)
                .build());

        new WeaviateFileSystemStack(app, "WeaviateFileSystemStack", StackProps.builder()
                .env(env)
                .build());

        new WeaviateStack(app, "WeaviateStack", StackProps.builder()
                .env(env)
                .build());

        app.synth();
    }
}
