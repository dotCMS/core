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

# libvips is copied from the java-base image, which builds it from source with the
# ImageMagick and OpenEXR delegates compiled out (docker/java-base/build-libvips.sh).
# Ubuntu's libvips42t64 is deliberately NOT installed: it hard-depends on
# libmagickcore-6.q16-7t64 and libopenexr-3-1-30, which carry every ImageMagick and
# OpenEXR CVE in the security report and have no fixed version in noble, so no apt
# upgrade could clear them. Nothing in dotCMS calls ImageMagick - that dependency
# existed only because libvips' magick delegate linked it.
#
# Copied before the apt steps below so the delegate packages can be installed and
# ldconfig can resolve libvips.so.42. The loader config travels with the prefix.
COPY --from=container-base /usr/local/libvips /usr/local/libvips
COPY --from=container-base /etc/ld.so.conf.d/libvips.conf /etc/ld.so.conf.d/libvips.conf

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
        tzdata \
        ca-certificates \
        libmimalloc2.0 \
        libjemalloc2 \
        openssl \
        libapr1 \
        libarchive-tools \
        libpq-dev \
        apt-transport-https && \
    rm -rf /var/lib/apt/lists/*

# Install the libvips runtime dependency closure from the manifest generated at
# build time by build-libvips.sh. It is derived from the resolved ELF dependencies
# of the library, the CLI and every dynamic module - plus the dlopened libheif
# codec plugins, which ELF inspection cannot see - so it carries exactly what
# libvips needs and deliberately excludes libmagickcore-6.q16-7t64,
# libopenexr-3-1-30 and imagemagick-6-common.
#
# This libvips backs the image engine selected by IMAGE_API_USE_LIBVIPS, and that
# engine is ON by default whenever the native library is loadable, which it now is.
# It provides AVIF/HEIC encode+decode (libheif with its AV1 codec plugin), PDF
# (poppler), SVG (librsvg), animated GIF (nsgif/cgif), WebP, JXL, JPEG2000, TIFF,
# PNG and JPEG, plus smartcrop. Formats only ImageMagick could read (ICO, DDS, DPX,
# XCF) are no longer available, and EXR is gone entirely.
RUN apt update && \
    xargs -r apt install -y --no-install-recommends \
        < /usr/local/libvips/share/runtime-packages.txt && \
    ldconfig && \
    rm -rf /var/lib/apt/lists/*

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
# Tomcat Native (tcnative) built for Tomcat 9 in the java-base image, so it is
# built once per java-base release and reused here. Runtime deps (libapr1,
# libssl3) come from the apt packages above.
COPY --from=container-base /usr/local/tomcat-native /usr/local/tomcat-native

# Assert the native image engine actually works in this image rather than assuming
# it: confirm the vulnerable packages are absent, that nothing links MagickCore or
# OpenEXR, that every module dependency resolves, and that representative
# encode/decode round trips (including AVIF) succeed. Runs after all apt operations
# on purpose. A wrong module path raises no error - libvips silently falls back to
# the pure-JVM engine and VipsParityTest self-skips - so this must be explicit.
RUN /usr/local/libvips/share/verify-libvips.sh

USER $USER_UID:$USER_GID
ENV JAVA_HOME="/java"
ENV PATH=$PATH:/java/bin:/usr/local/libvips/bin
# Makes the tcnative library visible to the JVM (System.loadLibrary("tcnative-1"))
# and libvips visible to the vips-ffm FFM binding. ldconfig already registers the
# libvips prefix through /etc/ld.so.conf.d/libvips.conf; this is belt-and-braces
# for anything that dlopens without consulting the cache.
ENV LD_LIBRARY_PATH="/usr/local/tomcat-native/lib:/usr/local/libvips/lib"

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
