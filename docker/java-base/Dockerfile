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
# downloads.apache.org only hosts the current release; older ones move to
# archive.apache.org, so fall back there once ${TCNATIVE_VERSION} is superseded.
RUN cd /tmp && \
    (wget -q "https://downloads.apache.org/tomcat/tomcat-connectors/native/${TCNATIVE_VERSION}/source/tomcat-native-${TCNATIVE_VERSION}-src.tar.gz" || \
     wget -q "https://archive.apache.org/dist/tomcat/tomcat-connectors/native/${TCNATIVE_VERSION}/source/tomcat-native-${TCNATIVE_VERSION}-src.tar.gz") && \
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
# libvips, built from source with the ImageMagick and OpenEXR delegates removed
# ----------------------------------------------
# Ubuntu 24.04's libvips42t64 hard-depends on libmagickcore-6.q16-7t64 and
# libopenexr-3-1-30. Those two packages account for every ImageMagick/OpenEXR CVE
# in the security report, and noble ships no fixed version of either, so no apt
# upgrade clears them. dotCMS never calls ImageMagick itself - the dependency
# exists only because libvips' magick delegate links it.
#
# Building libvips with -Dmagick=disabled -Dopenexr=disabled drops those
# delegates (and therefore the packages) while keeping AVIF/HEIC, PDF (poppler),
# SVG, GIF, WebP, JXL, JPEG2000, TIFF, PNG and JPEG. This mirrors what the image
# does for tcnative: build once here, reuse downstream rather than recompiling.
#
# The delegate libraries libvips still needs are installed as build deps and
# then marked manual by build-libvips.sh, so the apt-mark auto / autoremove below
# prunes the whole build toolchain without dragging the runtime closure with it.
#
# This runs after the cleanup step, so the acceptance gate is the last thing to
# touch the image and therefore verifies it after every apt operation. It needs
# curl re-installed because the cleanup step above purges it.
# The stale PGDG apt source is dropped before apt update. An earlier step adds it
# for postgresql-client-18 and then purges gnupg, which leaves the repository
# configured but its signing key unavailable; because that step also wipes
# /var/lib/apt/lists/* there are no cached indexes to fall back on, so apt update
# aborts with NO_PUBKEY. postgresql-client-18 is already installed and java-base
# never needs that repository again.
COPY build-libvips.sh verify-libvips.sh /usr/local/bin/
RUN set -eux; \
    BUILD_DEPS="meson ninja-build gcc g++ pkg-config gettext xz-utils binutils curl \
libglib2.0-dev libspng-dev libjpeg-dev libtiff-dev libwebp-dev libheif-dev \
libjxl-dev libopenjp2-7-dev libcgif-dev librsvg2-dev libcairo2-dev \
libpoppler-glib-dev liblcms2-dev libexif-dev libfontconfig1-dev libpango1.0-dev \
libarchive-dev libfftw3-dev libmatio-dev libopenslide-dev libcfitsio-dev \
libhwy-dev libimagequant-dev zlib1g-dev"; \
    rm -f /etc/apt/sources.list.d/pgdg.sources /etc/apt/sources.list.d/pgdg.list; \
    apt update; \
    apt install -y --no-install-recommends $BUILD_DEPS; \
    bash /usr/local/bin/build-libvips.sh; \
    apt-mark auto $BUILD_DEPS; \
    apt autoremove -y; \
    apt clean; \
    rm -rf /var/lib/apt/lists/*; \
    /usr/local/libvips/share/verify-libvips.sh

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch
COPY --from=base-builder / /

# tcnative built in base-builder; downstream images inherit it via COPY and
# redeclare this ENV themselves (ENVs do not survive the flatten to scratch).
ENV LD_LIBRARY_PATH="/usr/local/tomcat-native/lib"
