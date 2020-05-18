FROM openjdk:8-jdk-alpine

LABEL "repository"="https://github.com/dotCMS/core"
LABEL "homepage"="https://dotcms.com/"
LABEL "maintainer"="dotCMS <info@dotcms.com>"

LABEL com.dotcms.contact = "info@dotcms.com"
LABEL com.dotcms.vendor = "dotCMS LLC"
LABEL com.dotcms.description = "dotCMS Content Management System"

# Build env dependencies
RUN apk update && apk --no-cache add bash grep python py-crcmod openssh-client gnupg libc6-compat git nodejs-current nodejs-current-dev sed curl coreutils

# Location for assets, indexes and felix
RUN mkdir -p /common \
    && mkdir -p /common/core \
    && mkdir -p /common/enterprise \
    && mkdir -p /common/core-web

ENV CORE_REPOSITORY_FOLDER="/common/core/"
ENV ENTERPRISE_REPOSITORY_FOLDER="/common/enterprise/"
ENV CORE_WEB_REPOSITORY_FOLDER="/common/core-web/"

ARG RELEASE_VERSION
ENV RELEASE_VERSION=$RELEASE_VERSION
ENV RELEASE_BRANCH="release-${RELEASE_VERSION}"
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=$GITHUB_TOKEN

WORKDIR /srv

COPY setup/download.sh /build/download.sh
RUN chmod 500 /build/download.sh

COPY setup/validate.sh /build/validate.sh
RUN chmod 500 /build/validate.sh

COPY setup/entrypoint.sh /build/entrypoint.sh
RUN chmod 500 /build/entrypoint.sh

ENTRYPOINT ["/build/entrypoint.sh"]