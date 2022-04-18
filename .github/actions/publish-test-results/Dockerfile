FROM alpine:3.14.6 as results-base

ARG BRANCH=master
ENV BRANCH=${BRANCH}

WORKDIR /srv

RUN apk update \
    && apk --no-cache upgrade \
    && apk add --no-cache bash git curl

COPY build-src/entrypoint.sh .
COPY build-src/test-results.sh .
RUN find . -type f -name "*.sh" -exec chmod a+x {} \;

ENTRYPOINT ["/srv/entrypoint.sh"]
