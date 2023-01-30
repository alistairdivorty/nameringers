## Web App

- [What It Does](#1-what-it-does)
- [Local Setup](#2-local-setup)
  - [Prerequisites](#21-prerequisites)
  - [Set Up Environment](#22-set-up-environment)
- [Directory Structure](#3-directory-structure)
- [Deployment](#4-deployment)

### 1. What It Does

This is a React-based web application architected with the [Next.js](https://nextjs.org/) framework that allows users to perform similarity search across a dataset consisting of newly registered domain names by querying an index of vectors stored in an instance of the Weaviate vector database.

### 2. Local Setup

#### 2.1. Prerequisites

- [Node.js JavaScript runtime environment](https://nodejs.org/en/download/)

#### 2.2. Set Up Environment

Install the Node dependencies by running `npm install` from the `web-app` directory. Run `npm run dev` to start the local development server. By default the server is started on port 3000. Navigate to `http://localhost:3000` to view the site in a web browser.

To create a file for storing environment variables used by the CDK application during deployment of the web application, change the current working directory to `web-app/cdk` and run `cp .env.example .env`.

### 3. Directory Structure

```
📦web-app
 ┣ 📂cdk
 ┃ ┣ 📂bin
 ┃ ┃ ┗ 📜cdk.ts
 ┃ ┣ 📂lib
 ┃ ┃ ┗ 📜web-app-stack.ts
 ┃ ┣ 📜.env.example
 ┃ ┣ 📜.eslintrc.json
 ┃ ┣ 📜.gitignore
 ┃ ┣ 📜.npmignore
 ┃ ┣ 📜.prettierrc
 ┃ ┣ 📜cdk.context.json
 ┃ ┣ 📜cdk.json
 ┃ ┣ 📜jest.config.js
 ┃ ┣ 📜package-lock.json
 ┃ ┣ 📜package.json
 ┃ ┗ 📜tsconfig.json
 ┣ 📂nexjs-app
 ┃ ┣ 📂components
 ┃ ┣ 📂context
 ┃ ┣ 📂hooks
 ┃ ┣ 📂pages
 ┃ ┃ ┣ 📜_app.tsx
 ┃ ┃ ┣ 📜_document.tsx
 ┃ ┃ ┗ 📜index.tsx
 ┃ ┣ 📂public
 ┃ ┃ ┣ 📜favicon.ico
 ┃ ┃ ┗ 📜robots.txt
 ┃ ┣ 📂styles
 ┃ ┃ ┗ 📜globals.css
 ┃ ┣ 📂types
 ┃ ┃ ┗ 📜index.ts
 ┃ ┣ 📜.eslintrc.json
 ┃ ┣ 📜.gitignore
 ┃ ┣ 📜.prettierrc.json
 ┃ ┣ 📜next-env.d.ts
 ┃ ┣ 📜next.config.js
 ┃ ┣ 📜package-lock.json
 ┃ ┣ 📜package.json
 ┃ ┣ 📜postcss.config.js
 ┃ ┣ 📜tailwind.config.js
 ┃ ┗ 📜tsconfig.json
```

## 4. Deployment

The project is deployed via an AWS CDK application located in the `web-app/cdk` directory. The CDK app takes care of bundling the project files using the [standalone output](https://nextjs.org/docs/advanced-features/output-file-tracing) build mode for deployment to Lambda. To deploy the application using the [AWS CDK Toolkit](https://docs.aws.amazon.com/cdk/v2/guide/cli.html), change the current working directory to `web-app/cdk` and run `cdk deploy NameRingersWebFrontendStack`.
