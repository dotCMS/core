# ----------------------------------------------
# Stage 1:  Minimal java image with sdkman + Ubuntu LTS
# ----------------------------------------------
FROM ubuntu:24.04 AS base-builder

WORKDIR /srv

# Defining default Java version, can be any java version provided by sdkman
ARG SDKMAN_JAVA_VERSION="25.0.4+1-ms"
ARG TCNATIVE_VERSION="1.3.8"
ARG TCNATIVE_SHA512="a12b97979037720465300cbc05777ff6ee2ec1dc59ff698864d7068800bf0c2d2606a4e0e29a4d7b16f92c9130604a13e0cc27929a224c9ff77e532ac98a4695"

ENV JAVA_OUTPUT_DIR="/java"
ENV DEBIAN_FRONTEND=noninteractive
ENV PATH="$PATH:/java/bin"
ENV SDKMAN_DIR="/root/.sdkman"
ENV PATH="$SDKMAN_DIR/bin:$PATH"

# Installing basic packages and SDKMAN
RUN apt update && \
    apt upgrade -y && \
    apt install -y --no-install-recommends zip unzip wget tzdata tini ca-certificates openssl libapr1 libpq-dev curl gnupg \
        gcc make libapr1-dev libssl-dev && \
    rm -rf /var/lib/apt/lists/* && \
    wget -O - https://get.sdkman.io | bash && \
    bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java ${SDKMAN_JAVA_VERSION} && sdk flush archives" && \
    apt update && \
    apt install -y --no-install-recommends postgresql-common && \
    /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y && \
    apt install -y --no-install-recommends postgresql-client-18 && \
    apt purge -y postgresql-common gnupg && \
    /usr/bin/pg_dump --version || exit 1 && \
    rm -rf /var/lib/apt/lists/*

# Create a custom JRE using jlink with diagnostic tools
# Added JDK diagnostic modules for native memory tracking and profiling:
# - jdk.jcmd: jcmd command for VM.native_memory and other diagnostics
# - jdk.attach: Required for diagnostic tool attachment to running JVMs
# Note: jstack and jmap are included in jdk.jcmd in modern Java versions
RUN bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && jlink \
    --verbose \
    --add-modules \
        java.base,jdk.crypto.ec,jdk.jdwp.agent,jdk.management,jdk.management.agent,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported,java.scripting,java.rmi,jdk.compiler,jdk.zipfs,jdk.naming.dns,jdk.localedata,java.xml,jdk.xml.dom,jdk.jfr,jdk.management.jfr,jdk.jcmd,jdk.attach,java.net.http \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output \"$JAVA_OUTPUT_DIR\""

# Build the Tomcat Native library (tcnative) for Tomcat 9.
# Tomcat 9 requires the tcnative 1.x line (tcnative 2.x is Tomcat 10.1+ only).
# Ubuntu 24.04 only packages libtcnative-1 1.2.35, which is old and known to
# segfault with OpenSSL 3.x in FIPS environments, so we build the current
# Apache release from source, linked against this image's APR/OpenSSL 3.
# Built once here and reused by downstream images (the dotcms runtime image
# copies it from this image). Must run before the SDKMAN cleanup below,
# since the full JDK (JNI headers) is required. The build deps were installed
# with the Ubuntu repos only (before the pgdg repo is added), so no apt
# update is needed here; they are purged afterwards to keep the image slim.
# libpq-dev is purged here too because it depends on libssl-dev (purging
# libssl-dev removes it), and the cleanup step below no longer lists it.
# See https://tomcat.apache.org/download-native.cgi
RUN cd /tmp && \
    wget -q "https://downloads.apache.org/tomcat/tomcat-connectors/native/${TCNATIVE_VERSION}/source/tomcat-native-${TCNATIVE_VERSION}-src.tar.gz" && \
    echo "${TCNATIVE_SHA512} *tomcat-native-${TCNATIVE_VERSION}-src.tar.gz" | sha512sum -c - && \
    tar xzf "tomcat-native-${TCNATIVE_VERSION}-src.tar.gz" && \
    cd "tomcat-native-${TCNATIVE_VERSION}-src/native" && \
    ./configure --with-apr=/usr/bin/apr-1-config \
        --with-java-home="${SDKMAN_DIR}/candidates/java/current" \
        --with-ssl=/usr \
        --prefix=/usr/local/tomcat-native && \
    make -j"$(nproc)" && \
    make install && \
    cd / && rm -rf /tmp/tomcat-native-${TCNATIVE_VERSION}-src.tar.gz "tomcat-native-${TCNATIVE_VERSION}-src" && \
    apt purge -y gcc make libapr1-dev libssl-dev libpq-dev && \
    apt autoremove -y

# Cleanup
RUN rm -rf /root/.sdkman && \
    apt purge -y zip unzip wget curl && \
    apt autoremove -y && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch
COPY --from=base-builder / /

# tcnative built in base-builder; downstream images inherit it via COPY and
# redeclare this ENV themselves (ENVs do not survive the flatten to scratch).
ENV LD_LIBRARY_PATH="/usr/local/tomcat-native/lib"
