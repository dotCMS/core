# ----------------------------------------------
# Stage 1:  Minimal image with pg_dump
# ----------------------------------------------
FROM ubuntu:24.04 as pg-base-builder

SHELL ["/bin/bash", "-c"]
ARG DEBIAN_FRONTEND=noninteractive

# packages needed to install pg_dump
# ca-certificates included for base image completeness
ARG BUILD_PACKAGES="postgresql-common gnupg"
ARG RUNTIME_PACKAGES="ca-certificates"

# builds client only - see https://www.postgresql.org/docs/current/install-procedure.html
RUN apt update -y \
  && apt upgrade -y \
  && apt install -y --no-install-recommends $BUILD_PACKAGES $RUNTIME_PACKAGES \
  && /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y \
  && apt update -y \
  && apt install postgresql-client-16 -y \
  && apt remove -y $BUILD_PACKAGES \
  && apt purge -y \
  && apt autoremove -y \
  && apt clean \
  && rm -rf /var/lib/apt/lists/

RUN /usr/bin/pg_dump --version || exit 1

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

COPY --from=pg-base-builder / /
