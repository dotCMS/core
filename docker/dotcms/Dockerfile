# ----------------------------------------------
# Stage 1:  Build dotCMS from our builder image
# ----------------------------------------------
FROM dotcms/dotcms-seed:21.06 as build-cms

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

#  One of ["TARBALL_URL", "RELEASE", "NIGHTLY", "COMMIT", "TAG" ]
ARG BUILD_FROM=COMMIT

#  Value resolved in the context of $BUILD_FROM
ARG BUILD_ID=HEAD

WORKDIR /srv

# Set gradle options
ENV GRADLE_USER_HOME=/build/src/core/dotCMS/.gradle
ENV GRADLE_OPTS="-Dfile.encoding=utf-8 -Xmx2g"


# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
COPY build-src/ /build/
RUN chmod 500 /build/build_dotcms.sh && /build/build_dotcms.sh ${BUILD_FROM} ${BUILD_ID}

RUN mkdir -p /srv/utils /srv/templates /srv/config /srv/home
RUN chmod -R 666 /srv && find /srv/ -type d -exec chmod a+x {} \;

# ----------------------------------------------
# Stage 2:  Construct our container using the minimal-java image
#           and copying the prebuilt dotcms
# ----------------------------------------------
FROM dotcms/java-base:21.06 as container-base

WORKDIR /srv

ENV DEBIAN_FRONTEND=noninteractive

# Installing basic packages 
RUN apt update && \
    apt upgrade -y 

# Install prebuilt dockerize
RUN curl -Ls https://repo.dotcms.com/artifactory/ext-release-local/com/dotcms/dockerize/dockerize-linux-`uname -m` | install /dev/stdin /usr/local/bin/dockerize 

# Cleanup
RUN apt purge --allow-remove-essential -y  && \
    apt autoremove -y && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*

# Defining default non-root user UID, GID, and name 
ARG USER_UID="1000000000"
ARG USER_GID="1000000000"
ARG USER_NAME="dotcms"

# Creating default non-user 
# the useradd 
RUN groupadd -g $USER_GID $USER_NAME && \
	useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

# Copy our build
COPY --from=build-cms --chown=dotcms:dotcms /srv/ /srv/
COPY --chown=dotcms:dotcms ROOT/ /

# Make scripts runable
 RUN find /srv/ -type f -name "*.sh" -exec chmod a+x {} \; && \
    # Make plugin merging directories writable
    find /srv/templates -type d -exec chmod 770 {} \;  && \
    # Make dotcms user owner
    mkdir -p /data/shared/assets && \
    mkdir -p /data/local/dotsecure/license && \
    chown -R dotcms /data 

# ----------------------------------------------
# Stage 3:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

ENV SERVICE_DELAY_DEFAULT_MIN 1
ENV SERVICE_DELAY_DEFAULT_STEP 3
ENV SERVICE_DELAY_DEFAULT_MAX 30

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

ARG USER_UID="1000000000"
ARG USER_GID="1000000000"

COPY --from=container-base / /

# Switching to non-root user to install SDKMAN! 
USER $USER_UID:$USER_GID
ENV JAVA_HOME="/java"
ENV PATH=$PATH:/java/bin

ENTRYPOINT ["/usr/bin/tini", "--", "/srv/entrypoint.sh"]
CMD ["dotcms"]
    
## Ports
# Direct connect
EXPOSE 8080
# Direct connect for HTTPS, secure
EXPOSE 8443
# Connect from proxy, HTTP/80, non-secure
EXPOSE 8081
# Connect from proxy, HTTPS/443, secure
EXPOSE 8082
# Java Debugging - must be configured via CMS_JAVA_OPTS
EXPOSE 8000
# Glowroot profiler - must be configured via CMS_JAVA_OPTS
EXPOSE 4000