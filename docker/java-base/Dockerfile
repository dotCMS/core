# ----------------------------------------------
# Stage 1:  Minimal java image with sdkman + Ubuntu LTS
# ----------------------------------------------
FROM ubuntu:20.04 AS base-builder

WORKDIR /srv

# Defining default Java version, can be any java version provided by sdkman
ARG SDKMAN_JAVA_VERSION="11.0.22-ms"

ENV JAVA_OUTPUT_DIR="/java"
ENV DEBIAN_FRONTEND=noninteractive
ENV PATH="$PATH:/java/bin"
ENV SDKMAN_DIR="/root/.sdkman"
ENV PATH="$SDKMAN_DIR/bin:$PATH"

# Installing basic packages and SDKMAN
RUN apt update && \
    apt upgrade -y && \
    apt install -y --no-install-recommends zip unzip wget libtcnative-1 tzdata tini ca-certificates openssl libapr1 libpq-dev curl gnupg && \
    rm -rf /var/lib/apt/lists/* && \
    wget -O - https://get.sdkman.io | bash && \
    bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java ${SDKMAN_JAVA_VERSION} && sdk flush archives" && \
    apt update && \
    apt install -y --no-install-recommends postgresql-common && \
    /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y && \
    apt install -y --no-install-recommends postgresql-client-16 && \
    apt purge -y postgresql-common gnupg && \
    /usr/bin/pg_dump --version || exit 1 && \
    rm -rf /var/lib/apt/lists/*

# Create a custom JRE using jlink
RUN bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && jlink \
    --verbose \
    --add-modules \
        java.base,jdk.crypto.ec,jdk.jdwp.agent,jdk.management,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported,java.scripting,java.rmi,jdk.compiler,jdk.zipfs,jdk.naming.dns,jdk.localedata,java.xml,jdk.xml.dom \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output \"$JAVA_OUTPUT_DIR\""

# Cleanup
RUN rm -rf /root/.sdkman && \
    apt purge -y zip unzip wget curl libpq-dev && \
    apt autoremove -y && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch
COPY --from=base-builder / /
