# ----------------------------------------------
# Stage 1:  Build dotCMS from our builder image
# ----------------------------------------------
ARG DOTCMS_DOCKER_TAG="latest"

FROM dotcms/dotcms:${DOTCMS_DOCKER_TAG} AS dotcms

FROM ubuntu:24.04 AS dev-env-builder
# Defining default non-root user UID, GID, and name 
ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"
ARG DEV_REQUEST_TOKEN
ARG PG_VERSION=18
ENV PG_VERSION=${PG_VERSION}
RUN groupadd -f -g $USER_GID $USER_GROUP
# Creating default non-user
# the useradd
RUN useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

COPY --from=dotcms --chown=$USER_NAME:$USER_GROUP /srv/ /srv/
COPY --from=dotcms  /java /java
# Tomcat Native (tcnative) built for Tomcat 9 in the java-base image and
# shipped in the dotcms image; runtime deps (libapr1, libssl3) are in the apt
# packages below.
COPY --from=dotcms /usr/local/tomcat-native /usr/local/tomcat-native
# libvips, inherited from java-base through the dotcms image, where it is built from
# source with the ImageMagick and OpenEXR delegates removed
# (docker/java-base/build-libvips.sh). dev-env previously shipped no libvips at all,
# so the image engine silently ran on the legacy JVM path here; copying the prefix
# means dev-env now uses the same default engine as production. The loader config
# travels with the prefix.
COPY --from=dotcms /usr/local/libvips /usr/local/libvips
COPY --from=dotcms /etc/ld.so.conf.d/libvips.conf /etc/ld.so.conf.d/libvips.conf

ARG DEBIAN_FRONTEND=noninteractive
ARG UBUNTU_RELEASE=jammy
ARG PGDATA=/data/postgres
ARG DEBIAN_FRONTEND=noninteractive
ARG DEBCONF_NONINTERACTIVE_SEEN=true
RUN mkdir /data
RUN chmod 777 /data

# Installing basic packages
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends bash zip unzip wget \
    tzdata tini ca-certificates openssl libapr1 libpq-dev curl gnupg\
    vim libarchive-tools postgresql-common libmimalloc2.0 libjemalloc2 libarchive-tools


RUN /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y
RUN apt install -y --no-install-recommends postgresql-$PG_VERSION-pgvector



# Cleanup
RUN apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install the libvips runtime dependency closure recorded by build-libvips.sh in
# java-base. Ubuntu's libvips42 is deliberately not installed (it hard-depends on the
# CVE'd libmagickcore/libopenexr packages), so the closure comes from the generated
# manifest instead. This is the last apt operation in this stage, so the acceptance
# gate runs here and verifies the native engine the image will actually ship rather
# than assuming the copy worked.
RUN apt-get update && \
    xargs -r apt-get install -y --no-install-recommends \
        < /usr/local/libvips/share/runtime-packages.txt && \
    ldconfig && \
    rm -rf /var/lib/apt/lists/* && \
    /usr/local/libvips/share/verify-libvips.sh


COPY --from=opensearchproject/opensearch:1 --chown=$USER_NAME:$USER_GROUP /usr/share/opensearch /usr/share/opensearch


RUN echo "discovery.type: single-node\nbootstrap.memory_lock: true\ncluster.routing.allocation.disk.threshold_enabled: true\ncluster.routing.allocation.disk.watermark.low: 1g\ncluster.routing.allocation.disk.watermark.high: 500m\ncluster.routing.allocation.disk.watermark.flood_stage: 400m\ncluster.info.update.interval: 5m" >> /usr/share/opensearch/config/opensearch.yml

ENV OPENSEARCH_JAVA_HOME=/usr/share/opensearch/jdk
ENV JAVA_HOME=/java
ENV PATH=$PATH:/usr/share/opensearch/bin
RUN /usr/share/opensearch/opensearch-onetime-setup.sh
RUN chown -R dotcms.dotcms /usr/share/opensearch/config
COPY entrypoint.sh /
RUN chmod 755 /entrypoint.sh


FROM scratch
COPY --from=dev-env-builder  / /

# ENVs from dev-env-builder do not survive the flatten to scratch, so (re)declare
# what this change needs. LD_LIBRARY_PATH makes tcnative visible to the JVM
# (JAVA_HOME is exported by /entrypoint.sh) and libvips visible to the vips-ffm FFM
# binding; ldconfig already registers the libvips prefix via
# /etc/ld.so.conf.d/libvips.conf.
ENV LD_LIBRARY_PATH="/usr/local/tomcat-native/lib:/usr/local/libvips/lib"
# PATH is written out in full rather than appended to $PATH: this stage is
# scratch-based, so $PATH resolves to the builder's implicit default. That default
# is correct today, but relying on it is not obvious to a reader (two separate
# reviewers read it as expanding to an empty string and dropping /usr/bin). The
# value below is byte-identical to what the expansion produces.
ENV PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/libvips/bin"

ENTRYPOINT ["/usr/bin/tini", "--", "/entrypoint.sh"]
