FROM node:10.15.3 AS build

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

ARG NPM_TOKEN

WORKDIR /usr/src/app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build:libs

RUN echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > ~/.npmrc
RUN npm run publish:dev
RUN rm -f .npmrc

FROM node:8.11.3-alpine
RUN npm show dotcms-ui
