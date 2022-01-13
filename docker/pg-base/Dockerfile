# ----------------------------------------------
# Stage 1:  Minimal java image with pg_dump and its dependencies + Ubuntu LTS
# ----------------------------------------------
FROM ubuntu:20.04 as pg-base-builder

ENV DEBIAN_FRONTEND=noninteractive
ARG PG_VERSION=13
ENV PG_VERSION=$PG_VERSION

RUN apt-get update -y \
  && apt-get install -y gnupg gnupg1 gnupg2 wget lsb-release \
  && sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list' \
  && wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - \
  && apt-get update -y \
  && apt-get upgrade -y \
  && apt-get install -y postgresql-$PG_VERSION

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

ARG PG_VERSION=13
ENV PG_VERSION=$PG_VERSION

COPY --from=pg-base-builder /usr/lib/postgresql/$PG_VERSION/bin/pg_dump /usr/bin/
