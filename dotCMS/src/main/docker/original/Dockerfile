# ----------------------------------------------
# Stage 1: Construct our container using the minimal-java image and copying the prebuilt dotcms
# ----------------------------------------------
# Need to specify the SDKMAN_JAVA_VERSION to a valid sdkman java version that is available in the dotcms/java-base image
ARG SDKMAN_JAVA_VERSION="SDKMAN_JAVA_VERSION_ARG_NOT_SET"

FROM dotcms/java-base:${SDKMAN_JAVA_VERSION} AS container-base
WORKDIR /srv

ENV DEBIAN_FRONTEND=noninteractive
ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"

# Ensure group and user are created with proper permissions
RUN groupadd -g "$USER_GID" "$USER_GROUP" && \
    useradd -l -d /srv -u "$USER_UID" -g "$USER_GROUP" -s /bin/bash "$USER_NAME" && \
    mkdir -p /srv/utils /srv/templates /srv/config /srv/home /data/shared/assets /data/shared/felix/load /data/shared/felix/undeployed /data/local/dotsecure/license && \
    chown -R "$USER_NAME:$USER_GROUP" /data && \
    chown -R "$USER_NAME:$USER_GROUP" /srv

# Copy our build
COPY --chown=$USER_NAME:$USER_GROUP maven /srv/
COPY --chown=$USER_NAME:$USER_GROUP ROOT/ /

RUN ln -s $(ls -d /srv/dotserver/tomcat-*) /srv/dotserver/tomcat && \
    # Make scripts runnable
    find /srv/ -type f -name "*.sh" -exec chmod a+x {} \; && \
    # Make plugin merging directories writable
    find /srv/templates -type d -exec chmod 770 {} \;

# ----------------------------------------------
# Stage 2: Final stage for minimal runtime image
# ----------------------------------------------
FROM ubuntu:24.04

LABEL com.dotcms.contact="support@dotcms.com" \
      com.dotcms.vendor="dotCMS LLC" \
      com.dotcms.description="dotCMS Content Management System"

# Install basic packages and tini
RUN apt update && \
    apt upgrade -y && \
    apt install -y --no-install-recommends \
        wget \
        curl \
        gnupg \
        tini \
        zip \
        unzip \
        libtcnative-1 \
        tzdata \
        ca-certificates \
        libmimalloc2.0 \
        libjemalloc2 \
        openssl \
        libapr1 \
        libarchive-tools \
        libpq-dev \
        libvips42 \
        apt-transport-https && \
    rm -rf /var/lib/apt/lists/*

# libvips42 (Ubuntu 24.04 = libvips 8.15.1) backs the optional libvips image engine
# (IMAGE_API_USE_LIBVIPS=true). The stock package bundles the PDF (poppler), AVIF/HEIC
# (libheif), SVG (rsvg), WebP and JPEG-XL delegates, so no custom build is needed.
# The engine is off by default; when off this library is simply unused.

# Install PostgreSQL client, pg_dump and MS jaz (java launcher).
# postgresql-common is installed and purged in the same layer so it does not
# bloat the final image (it is only needed as a script runner for pgdg setup).
RUN apt-get update && \
    apt-get install -y --no-install-recommends postgresql-common && \
    /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y && \
    wget -q "https://packages.microsoft.com/config/ubuntu/$(. /etc/os-release; echo $VERSION_ID)/packages-microsoft-prod.deb" -O packages-microsoft-prod.deb && \
    dpkg -i packages-microsoft-prod.deb && \
    rm packages-microsoft-prod.deb && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        postgresql-client-18 \
        jaz && \
    /usr/bin/pg_dump --version && \
    apt-get purge -y postgresql-common && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"

# Ensure the group and user are created in the final image as well
RUN groupadd -g "$USER_GID" "$USER_GROUP" && \
    useradd -l -d /srv -u "$USER_UID" -g "$USER_GROUP" -s /bin/bash "$USER_NAME"

COPY --from=container-base /java /java
COPY --from=container-base /srv /srv
COPY --from=container-base /data /data

USER $USER_UID:$USER_GID
ENV JAVA_HOME="/java"
ENV PATH=$PATH:/java/bin

ENTRYPOINT ["/usr/bin/tini", "--", "/srv/entrypoint.sh"]
CMD ["dotcms"]

## Ports
# Glowroot profiler - must be configured via CMS_JAVA_OPTS
EXPOSE 4000
# Java Debugging - must be configured via CMS_JAVA_OPTS
EXPOSE 8000
# JMX Remote Monitoring - must be configured via CMS_JAVA_OPTS
EXPOSE 9999
# JMX RMI Port - must be configured via CMS_JAVA_OPTS  
EXPOSE 9998
# Direct connect
EXPOSE 8080
# Connect from proxy, HTTP/80, non-secure
EXPOSE 8081
# Connect from proxy, HTTPS/443, secure
EXPOSE 8082
# Direct connect for HTTPS, secure
EXPOSE 8443
