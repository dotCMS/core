# ----------------------------------------------
# Stage 1: Minimal java image with sdkman + Ubuntu LTS
# ----------------------------------------------
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu as base-builder

WORKDIR /srv

# Environment variables for Java and Debian frontend
ENV JAVA_OUTPUT_DIR="/java"
ENV DEBIAN_FRONTEND=noninteractive
ENV PATH="$PATH:/java/bin"

# Installing basic packages with security in mind
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    zip unzip wget libtcnative-1 tzdata tini ca-certificates openssl libapr1 libpq-dev && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install curl securely
RUN wget -qO /usr/bin/curl https://repo.dotcms.com/artifactory/ext-release-local/com/dotcms/curl-static/curl-`uname -m` && \
    chmod a+x /usr/bin/curl

# Create a minimized Java runtime image
RUN jlink \
    --verbose \
    --add-modules \
        java.base,jdk.crypto.ec,jdk.jdwp.agent,jdk.management,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported,java.scripting,java.rmi,jdk.compiler,jdk.zipfs,jdk.naming.dns,jdk.localedata,java.xml,jdk.xml.dom \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output "$JAVA_OUTPUT_DIR"

# Install PostgreSQL client and clean up
ARG PG_BUILD_PACKAGES="postgresql-common gnupg"
RUN apt-get update && \
    apt-get install -y --no-install-recommends $PG_BUILD_PACKAGES && \
    /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y && \
    apt-get install -y postgresql-client-16 && \
    apt-get purge -y $PG_BUILD_PACKAGES && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Ensure pg_dump is installed correctly
RUN /usr/bin/pg_dump --version || exit 1

# Cleanup unnecessary packages
RUN apt-get purge -y zip unzip wget fontconfig-config && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# ----------------------------------------------
# Stage 2: Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch
COPY --from=base-builder / /