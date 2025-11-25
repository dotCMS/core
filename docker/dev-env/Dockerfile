# ----------------------------------------------
# Stage 1:  Build dotCMS from our builder image
# ----------------------------------------------
ARG DOTCMS_DOCKER_TAG="latest"

FROM dotcms/dotcms:${DOTCMS_DOCKER_TAG} AS dotcms

FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS dev-env-builder

# Defining default non-root user UID, GID, and name 
ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"
ARG DEV_REQUEST_TOKEN
ARG PG_VERSION=16
ENV PG_VERSION=${PG_VERSION}
RUN groupadd -f -g $USER_GID $USER_GROUP
# Creating default non-user
# the useradd
RUN useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

COPY --from=dotcms --chown=$USER_NAME:$USER_GROUP /srv/ /srv/
COPY --from=dotcms  /java /java

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
    apt-get install -y --no-install-recommends bash zip unzip wget libtcnative-1\
    tzdata tini ca-certificates openssl libapr1 libpq-dev curl gnupg\
    vim libarchive-tools postgresql-common



RUN /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y
RUN apt install -y postgresql-$PG_VERSION-pgvector



# Cleanup
RUN apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


COPY --from=opensearchproject/opensearch:1 --chown=$USER_NAME:$USER_GROUP /usr/share/opensearch /usr/share/opensearch





RUN echo "discovery.type: single-node\nbootstrap.memory_lock: true\ncluster.routing.allocation.disk.threshold_enabled: true\ncluster.routing.allocation.disk.watermark.low: 1g\ncluster.routing.allocation.disk.watermark.high: 500m\ncluster.routing.allocation.disk.watermark.flood_stage: 400m\ncluster.info.update.interval: 5m" >> /usr/share/opensearch/config/opensearch.yml


ENV PATH=$PATH:/usr/share/opensearch/bin
RUN /usr/share/opensearch/opensearch-onetime-setup.sh
RUN rm -rf /usr/share/opensearch/jdk
RUN chown -R dotcms.dotcms /usr/share/opensearch/config
COPY entrypoint.sh /
RUN chmod 755 /entrypoint.sh

## This file is used to request a dev license from dotCMS
COPY license-request.sh /
RUN chmod 755 /license-request.sh
RUN bash -x /license-request.sh
RUN rm /license-request.sh

FROM scratch
COPY --from=dev-env-builder  / /

ENTRYPOINT ["/usr/bin/tini", "--", "/entrypoint.sh"]
