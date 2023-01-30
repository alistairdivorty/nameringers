## Zone File Client

- [What It Does](#1-what-it-does)
- [Local Setup](#2-local-setup)
  - [Prerequisites](#21-prerequisites)
- [Directory Structure](#3-directory-structure)
- [Deployment](#4-deployment)
- [Invoke Lambda Function in Production Environment](#5-invoke-lambda-function-in-production-environment)

### 1. What It Does

This is a Lambda function written in Java that fetches compressed TXT files containing domain names extracted from the zone files for generic top-level domains, decompresses the data and uploads them to an S3 bucket for retrieval by the feature extraction pipeline. To ensure that the Lambda memory limit is not exceeded as a result of decompressing large files, data are written to an EFS volume mounted onto the function rather than the ephemeral Lambda file system.

### 2. Local Setup

#### 2.1. Prerequisites

- [OpenJDK 11](https://adoptopenjdk.net/releases.html)
- [Apache Maven](https://maven.apache.org/index.html)

### 3. Directory Structure

```
📦zonefile-client
 ┣ 📂src
 ┃ ┣ 📂main
 ┃ ┃ ┗ 📂java
 ┃ ┃ ┃ ┗ 📂com
 ┃ ┃ ┃ ┃ ┗ 📂nameringers
 ┃ ┃ ┃ ┃ ┃ ┗ 📂zonefile
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜App.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜DependencyFactory.java
 ┣ 📂target
 ┣ 📜.gitignore
 ┣ 📜pom.xml
 ┗ 📜template.yaml
```

### 4. Deployment

The [AWS CDK app](#5-aws-cdk-app) takes care of bundling the project files and dependencies into an assembly jar for deployment to Lambda. To deploy the application using the [AWS CDK Toolkit](https://docs.aws.amazon.com/cdk/v2/guide/cli.html), change the current working directory to `cdk` and run `cdk deploy ZoneFileStack`. See the [AWS CDK app](../README.md#5-aws-cdk-app) section of the main README for details of how to set up the AWS CDK Toolkit.

### 5. Invoke Lambda Function in Production Environment

The following example shows how to invoke the Lambda function using the AWS CLI. The function output will be saved to a file named `response.json`.

```shell
aws lambda invoke \
  --function-name <function-name> \
  --payload '{ "zone": "com" }' \
  response.json
```
