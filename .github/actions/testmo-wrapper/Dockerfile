FROM node:16.15.0-alpine as results-base

WORKDIR /srv

RUN apk update \
    && apk --no-cache upgrade \
    && apk add --no-cache bash git curl

COPY build-src/ .
RUN find . -type f -name "*.sh" -exec chmod a+x {} \;

ENTRYPOINT ["/srv/entrypoint.sh"]
