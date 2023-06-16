# ----------------------------------------------
# Stage 1:  Build dotCMS from our builder image
# ----------------------------------------------
#FROM dotcms/dotcms:latest as dotcms


FROM ubuntu:22.04

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

# Defining default non-root user UID, GID, and name 
ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"
ARG USER_NAME="dotcms"
RUN groupadd -f -g $USER_GID $USER_GROUP
# Creating default non-user
# the useradd
RUN useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

COPY --from=dotcms/dotcms:latest --chown=$USER_NAME:$USER_GROUP /srv/ /srv/

ARG DEBIAN_FRONTEND=noninteractive
ENV PG_VERSION=14
ENV PGDATA=/data/postgres
RUN mkdir /data
RUN chmod 777 /data

# Installing basic packages 
RUN apt update && \
    apt upgrade -y && \
    apt install -y --no-install-recommends bash zip unzip wget libtcnative-1\
    tzdata tini ca-certificates openssl libapr1 libpq-dev curl gnupg\
    vim libarchive-tools 
RUN sh -c sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'

RUN wget -qO- https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo tee /etc/apt/trusted.gpg.d/pgdg.asc &>/dev/null \
  && apt-get update -y \
  && apt-get upgrade -y \
  && apt-get install -y postgresql-$PG_VERSION




COPY --from=opensearchproject/opensearch:1 /usr/share/opensearch /usr/share/opensearch

RUN chown -R dotcms.dotcms /usr/share/opensearch
RUN echo "discovery.type: single-node" >> /usr/share/opensearch/config/opensearch.yml
RUN echo "bootstrap.memory_lock: true" >> /usr/share/opensearch/config/opensearch.yml
RUN echo "cluster.routing.allocation.disk.threshold_enabled: true" >> /usr/share/opensearch/config/opensearch.yml
RUN echo "cluster.routing.allocation.disk.watermark.low: 1g" >> /usr/share/opensearch/config/opensearch.yml
RUN echo "cluster.routing.allocation.disk.watermark.high: 500m" >> /usr/share/opensearch/config/opensearch.yml
RUN echo "cluster.routing.allocation.disk.watermark.flood_stage: 400m" >> /usr/share/opensearch/config/opensearch.yml
RUN echo "cluster.info.update.interval: 5m" >> /usr/share/opensearch/config/opensearch.yml




ENV PATH=$PATH:/usr/share/opensearch/bin
RUN /usr/share/opensearch/opensearch-onetime-setup.sh

COPY entrypoint.sh /
RUN chmod 755 /entrypoint.sh

ENTRYPOINT ["/usr/bin/tini", "--", "/entrypoint.sh"]
