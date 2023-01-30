## AWS CDK App

- [What It Does](#1-what-it-does)
- [Local Setup](#2-local-setup)
  - [Prerequisites](#21-prerequisites)
  - [Set Up Environment](#22-set-up-environment)
- [Directory Structure](#3-directory-structure)
- [Deployment](#4-deployment)

### 1. What It Does

The [AWS Cloud Development Kit (CDK)](https://docs.aws.amazon.com/cdk/v2/guide/home.html) is a framework for defining cloud infrastructure in code and provisioning it through [AWS CloudFormation](https://aws.amazon.com/cloudformation/). This is an AWS CDK application that defines the cloud infrastructure required by the services contained in this repository.

### 2. Local Setup

#### 2.1. Prerequisites

- [Node.js JavaScript runtime environment](https://nodejs.org/en/download/)

#### 2.2. Set Up Environment

To install the [CDK Toolkit](https://docs.aws.amazon.com/cdk/v2/guide/cli.html) (a CLI tool for interacting with a CDK app) using the [Node Package Manager](https://www.npmjs.com/), run the command `npm install -g aws-cdk`. The CDK Toolkit needs access to AWS credentials. Access to your credentials can be configured using the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) by running `aws configure` and following the prompts.

Install the Node dependencies by running `npm install` from the `cdk` directory.

To create a file for storing environment variables, run `cp .env.example .env`.

### 3. Directory Structure

```
📦cdk
 ┣ 📂src
 ┃ ┣ 📂main
 ┃ ┃ ┗ 📂java
 ┃ ┃ ┃ ┗ 📂com
 ┃ ┃ ┃ ┃ ┗ 📂nameringers
 ┃ ┃ ┃ ┃ ┃ ┣ 📜CdkApp.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📜EMRServerlessStack.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📜WeaviateClientStack.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📜WeaviateFileSystemStack.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📜WeaviateStack.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📜ZoneFileStack.java
 ┣ 📂target
 ┣ 📜.env.example
 ┣ 📜.gitignore
 ┣ 📜cdk.context.json
 ┣ 📜cdk.json
 ┣ 📜pom.xml
 ┗ 📜weaviate.Dockerfile
```

### 4. Deployment

To deploy all the stacks defined by the application, change the current working directory to `cdk` and run `cdk deploy --all`.
