## Feature Extraction Pipeline

- [What It Does](#1-what-it-does)
- [Local Setup](#2-local-setup)
  - [Prerequisites](#21-prerequisites)
  - [Set Up Environment](#22-set-up-environment)
- [Directory Structure](#3-directory-structure)
- [Run Job in Local Development Environment](#4-run-job-in-local-development-environment)
- [Deployment](#5-deployment)
- [Run Job in Production Environment](#6-run-job-in-production-environment)

### 1. What It Does

This is a feature extraction pipeline written in Scala that uses [Spark's machine learning library](https://spark.apache.org/docs/latest/ml-guide.html) to generate feature vectors for a text corpus consisting of domain names.

### 2. Local Setup

#### 2.1. Prerequisites

- [OpenJDK 11](https://adoptopenjdk.net/releases.html)
- [sbt](https://www.scala-sbt.org/download.html)
- [Docker](https://www.docker.com/)

#### 2.2. Set Up Environment

If your system runs on the Apple Silicon M1 processor, disable fork safety by adding the following line to your shell initialisation file.

```shell
export OBJC_DISABLE_INITIALIZE_FORK_SAFETY=YES
```

The [Hadoop-AWS module](https://hadoop.apache.org/docs/stable/hadoop-aws/tools/hadoop-aws/index.html) requires access to AWS credentials with permission to write data to S3. Access to your credentials can be configured using the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) by running `aws configure` and following the prompts.

To start a local instance of the [Weaviate vector database](https://weaviate.io/) using [Docker Compose](https://docs.docker.com/compose/), paste the Compose specification below into a local `docker-compose.yml` file and run the command `docker-compose up` from the directory containing the YML file. The database server will be reachable at the hostname `localhost:8080`.

```yml
version: "3.4"
services:
  weaviate:
    image: semitechnologies/weaviate:1.14.0
    ports:
      - 8080:8080
    restart: on-failure:0
    environment:
      QUERY_DEFAULTS_LIMIT: 25
      AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: "true"
      PERSISTENCE_DATA_PATH: "/var/lib/weaviate"
      DEFAULT_VECTORIZER_MODULE: "none"
      CLUSTER_HOSTNAME: "node1"
```

### 3. Directory Structure

```
????spark
 ??? ????lib
 ??? ??? ????weaviate-spark-connector-assembly-v0.1.2.jar
 ??? ????project
 ??? ????target
 ??? ????src
 ??? ??? ????main
 ??? ??? ??? ????scala
 ??? ??? ??? ??? ????spark
 ??? ??? ??? ??? ??? ????DomainNames.scala
 ??? ????.gitignore
 ??? ????Dockerfile
 ??? ????build.sbt
```

### 4. Run Job in Local Development Environment

Execute the following command from the `spark` directory to launch a Spark session in standalone mode and run the `DomainNames` job in the same virtual machine as sbt.

```shell
sbt "run <zone-file-uri> <bucket-name> <weaviate-host>"
```

The placeholder values for the script arguments refer to:

- a path to a TXT file generated by the Zone File Client application documented below;
- the name of an S3 bucket for storing the serialised model that can be accessed using AWS credentials configured for your local environment; and
- the hostname for a local or remote instance of the Weaviate vector database.

### 5. Deployment

An assembly jar containing the project files and dependencies needs to be uploaded to an S3 bucket from where it can be downloaded by the [EMR Serverless](https://aws.amazon.com/emr/serverless/) application when running in production. The [AWS CDK app](#5-aws-cdk-app) takes care of bundling the source code and uploading the deployment artifact. To deploy the application using the [AWS CDK Toolkit](https://docs.aws.amazon.com/cdk/v2/guide/cli.html), change the current working directory to `cdk` and run `cdk deploy NameRingersEMRServerlessStack`. See the [AWS CDK app](../README.md#5-aws-cdk-app) section of the main README for details of how to set up the AWS CDK Toolkit. The AWS CDK app outputs the ID of the EMR Serverless application created by the CloudFormation stack, the [ARN](https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html) for the [IAM](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html) execution role, and S3 URIs for the assembly jar and logs folder.

### 6. Run Job in Production Environment

The following is an example of how to submit a job to the [EMR Serverless](https://docs.aws.amazon.com/emr/latest/EMR-Serverless-UserGuide/emr-serverless.html) application deployed by the [AWS CDK app](#5-aws-cdk-app) using the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html). The placeholder values should be replaced with the values outputted by the CDK app after deployment of the `NameRingersEMRServerlessStack` and `WeaviateStack` stacks.

```shell
aws emr-serverless start-job-run \
    --region eu-west-1 \
    --application-id <application-ID> \
    --execution-role-arn <role-ARN> \
    --job-driver '{
        "sparkSubmit": {
            "entryPoint": <assembly-jar-URI>,
            "entryPointArguments": [<zone-file-URI>, <bucket-name>, <weaviate-host>],
            "sparkSubmitParameters": "--class com.nameringers.spark.DomainNames --conf spark.executorEnv.JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.16.0.8-1.amzn2.0.1.x86_64 --conf spark.emr-serverless.driverEnv.JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.16.0.8-1.amzn2.0.1.x86_64 --conf spark.dynamicAllocation.enabled=false --conf spark.executor.instances=2 --conf spark.driver.cores=2 --conf spark.executor.cores=2"
        }
    }' \
    --configuration-overrides '{
        "monitoringConfiguration": {
            "s3MonitoringConfiguration": {
                "logUri": <logs-URI>
            }
        }
    }'
```

The `spark.emr-serverless.driverEnv.JAVA_HOME` and `spark.executorEnv.JAVA_HOME` configuration properties refer to an image stored in [ECR](https://aws.amazon.com/ecr/) that is used to package the specific JDK version required by the project. The `spark` directory contains a `Dockerfile` with instructions for building the JDK 11 image.
