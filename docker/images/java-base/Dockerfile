# ----------------------------------------------
# Stage 1:  Minimal java image with sdkman + Ubuntu LTS
# ----------------------------------------------
FROM ubuntu:20.04 as base-builder

WORKDIR /srv

# Defining default Java, can be any java provided by sdkman
ARG JAVA_VERSION="8.0.352-amzn"

ENV JAVA_OUTPUT_DIR="/java"
ENV DEBIAN_FRONTEND=noninteractive
ENV PATH="$PATH:/java/bin"

# Installing basic packages 
RUN apt update && \
    apt upgrade -y && \
    apt install -y --no-install-recommends zip unzip wget libtcnative-1 tzdata tini ca-certificates openssl libapr1 libpq-dev


# Install curl
RUN wget -O - https://repo.dotcms.com/artifactory/ext-release-local/com/dotcms/curl-static/curl-`uname -m` | install /dev/stdin /usr/bin/curl && \
    chmod a+x /usr/bin/curl 

# Downloading sdkman
RUN curl -sL "https://get.sdkman.io" | bash

RUN bash -c "source /root/.sdkman/bin/sdkman-init.sh && \
    sdk list java && \
    yes | sdk install java $JAVA_VERSION "
    
RUN bash -c "mv /root/.sdkman/candidates/java/$JAVA_VERSION /java"

    
# Cleanup
RUN rm -rf /root/.sdkman && \
    apt purge -y zip unzip wget && \
    apt autoremove -y && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch
COPY --from=base-builder / /

