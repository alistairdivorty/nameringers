#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { WebAppStack } from '../lib/web-app-stack';
import * as dotenv from 'dotenv';

dotenv.config();

const env: cdk.Environment = {
    account: process.env.AWS_ACCOUNT_ID,
    region: 'eu-west-1'
};

const app = new cdk.App();

new WebAppStack(app, 'NameRingersWebFrontendStack', { env });
