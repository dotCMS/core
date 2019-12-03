FROM node:10.15.3

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

# Installing gcloud
# (https://github.com/GoogleCloudPlatform/cloud-sdk-docker/blob/master/alpine/Dockerfile)
# https://cloud.google.com/sdk/docs/
ARG CLOUD_SDK_VERSION=256.0.0
ENV CLOUD_SDK_VERSION=$CLOUD_SDK_VERSION
ENV PATH /google-cloud-sdk/bin:$PATH
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    tar xzf google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    rm google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    gcloud config set core/disable_usage_reporting true && \
    gcloud config set component_manager/disable_update_check true && \
    gcloud config set metrics/environment github_docker_image && \
    gcloud --version
#
    
WORKDIR /usr/src/app

RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium
ENV CHROME_BIN=chromium

RUN npm i -g @angular/cli@7.1.4

COPY package*.json ./
RUN npm ci

COPY angular.json .

COPY tsconfig.json .

COPY ./projects/dotcms-js ./projects/dotcms-js  
RUN ng build dotcms-js

COPY ./projects/dot-layout-grid ./projects/dot-layout-grid 
RUN ng build dot-layout-grid

COPY ./projects/dot-rules ./projects/dot-rules 

COPY karma.conf.js .

COPY ./src ./src

COPY ./docker ./testing
RUN chmod -R 500 ./testing


