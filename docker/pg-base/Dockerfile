# ----------------------------------------------
# Stage 1:  Minimal image with pg_dump and psql built from source 
# ----------------------------------------------
FROM ubuntu:20.04 as pg-base-builder

SHELL ["/bin/bash", "-c"] 
ARG DEBIAN_FRONTEND=noninteractive

# see https://www.postgresql.org/ftp/source/ for versions
ARG POSTGRES_VERSION=15.3

ARG POSTGRES_TARBALL=postgresql-${POSTGRES_VERSION}.tar.bz2
ARG FTP_PATH=https://ftp.postgresql.org/pub/source/v${POSTGRES_VERSION}

# zlib1g-dev is required for pg_dump to support compression
ARG BUILD_DEPS="build-essential ca-certificates curl zlib1g-dev"

# './configure --without-readline' currently means we won't have shared library
# dependencies that are not present in the dotcms image
ARG CONFIGURE_OPTS="--without-readline"

# builds client only - see https://www.postgresql.org/docs/current/install-procedure.html
RUN apt update -y \
  && apt upgrade -y \
  && apt install -y --no-install-recommends ${BUILD_DEPS} \
  && mkdir -p /postgres/build \
  && cd /postgres/build \
  && curl -o ${POSTGRES_TARBALL} ${FTP_PATH}/${POSTGRES_TARBALL} \
  && diff <(md5sum postgresql-${POSTGRES_VERSION}.tar.bz2) <(curl -s ${FTP_PATH}/${POSTGRES_TARBALL}.md5) || exit 1 \
  && tar xf ${POSTGRES_TARBALL} \
  && cd /postgres/build/postgresql-${POSTGRES_VERSION} \
  && ./configure ${CONFIGURE_OPTS} \
  && make -C src/bin/pg_dump install \
  && make -C src/bin/psql install \
  && make -C src/include install \
  && make -C src/interfaces install \
  && rm -rf /postgres/build \
  && apt remove -y ${BUILD_DEPS} \
  && apt purge -y \
  && apt autoremove -y \
  && apt clean \
  && rm -rf /var/lib/apt/lists/
RUN /usr/local/pgsql/bin/pg_dump --version || exit 1

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

COPY --from=pg-base-builder / /