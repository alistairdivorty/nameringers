FROM --platform=linux/amd64 public.ecr.aws/emr-serverless/spark/emr-6.9.0:latest

USER root

# install JDK 11
RUN amazon-linux-extras install java-openjdk11

# EMRS will run the image as hadoop
USER hadoop:hadoop