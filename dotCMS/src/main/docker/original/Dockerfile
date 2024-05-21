# ----------------------------------------------
# Stage 2:  Construct our container using the minimal-java image
#           and copying the prebuilt dotcms
# ----------------------------------------------
FROM dotcms/java-base:ms-jdk-21 as container-base

WORKDIR /srv

ENV DEBIAN_FRONTEND=noninteractive

# Installing basic packages 
RUN apt update \
    && apt upgrade -y

# Cleanup
RUN apt purge --allow-remove-essential -y \
    && apt autoremove -y \
    && apt clean \
    && rm -rf /var/lib/apt/lists/*

# Defining default non-root user UID, GID, and name 
ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"

RUN groupadd -f -g $USER_GID $USER_GROUP
# Creating default non-user
# the useradd
RUN useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

RUN mkdir -p /srv/utils /srv/templates /srv/config /srv/home

# Copy our build
COPY --chown=$USER_NAME:$USER_GROUP maven /srv/
COPY --chown=$USER_NAME:$USER_GROUP ROOT/ /

RUN ln -s $(ls -d /srv/dotserver/tomcat-*) /srv/dotserver/tomcat

# Make scripts runable
RUN find /srv/ -type f -name "*.sh" -exec chmod a+x {} \; && \
    # Make plugin merging directories writable
    find /srv/templates -type d -exec chmod 770 {} \;  && \
    # Make dotcms user owner
    mkdir -p /data/shared/assets && \
    mkdir -p /data/shared/felix/load && \
    mkdir -p /data/shared/felix/undeployed && \
    mkdir -p /data/local/dotsecure/license && \
    chown -R $USER_NAME:$USER_NAME /data

# ----------------------------------------------
# Stage 3:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

ARG USER_UID="65001"
ARG USER_GID="65001"

COPY --from=container-base / /

# Switching to non-root user to install SDKMAN!
USER $USER_UID:$USER_GID
ENV JAVA_HOME="/java"
ENV PATH=$PATH:/java/bin

ENTRYPOINT ["/usr/bin/tini", "--", "/srv/entrypoint.sh"]
CMD ["dotcms"]

## Ports
# Glowroot profiler - must be configured via CMS_JAVA_OPTS
EXPOSE 4000
# Java Debugging - must be configured via CMS_JAVA_OPTS
EXPOSE 8000
# Direct connect
EXPOSE 8080
# Connect from proxy, HTTP/80, non-secure
EXPOSE 8081
# Connect from proxy, HTTPS/443, secure
EXPOSE 8082
# Direct connect for HTTPS, secure
EXPOSE 8443
