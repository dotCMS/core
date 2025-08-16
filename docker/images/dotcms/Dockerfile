
# ----------------------------------------------
# Stage 1:  Build dotCMS from our builder image
# ----------------------------------------------
FROM dotcms/dotcms-seed:5.1.6ks as build-cms

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

#  One of ["TARBALL_URL", "RELEASE", "NIGHTLY", "COMMIT", "TAG" ]
ARG BUILD_FROM=COMMIT

#  Value resolved in the context of $BUILD_FROM
ARG BUILD_ID=HEAD

RUN echo "BUILD_FROM: $BUILD_FROM / BUILD_ID: $BUILD_ID"

WORKDIR /srv

# Set gradle options
ENV GRADLE_USER_HOME=/build/src/core/dotCMS/.gradle
ENV GRADLE_OPTS="-Dfile.encoding=utf-8 -Xmx2g"
ENV JAVA_HOME="/usr/lib/jvm/java"

# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
COPY build-src/ /build/

RUN echo "building: ${BUILD_FROM} ${BUILD_ID}"
RUN chmod a+x /build/build_dotcms.sh && /build/build_dotcms.sh ${BUILD_FROM} ${BUILD_ID}

RUN mkdir -p /srv/utils /srv/templates /srv/config /srv/home
RUN chmod -R 666 /srv && find /srv/ -type d -exec chmod a+x {} \;

# ensure only one tomcat directory exists
RUN ["/bin/bash", "-c", "[ $(ls -d /srv/dotserver/tomcat-* | wc -l) == 1 ] || exit 1" ]
# provide stable path to tomcat dir
RUN ln -s $(ls -d /srv/dotserver/tomcat-*) /srv/dotserver/tomcat

# ----------------------------------------------
# Stage 2:  Construct our container using the minimal-java image
#           and copying the prebuilt dotcms
# ----------------------------------------------
FROM dotcms/fedora-java-base:8-shenandoah as container-base

WORKDIR /srv

ENV DEBIAN_FRONTEND=noninteractive



# Defining default non-root user UID, GID, and name 
ARG USER_UID="65001"
ARG USER_GID="65001"
ARG USER_GROUP="dotcms"
ARG USER_NAME="dotcms"

RUN groupadd -f -g $USER_GID $USER_GROUP
# Creating default non-user
# the useradd
RUN useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

# Copy our build
COPY --from=build-cms --chown=$USER_NAME:$USER_GROUP /srv/ /srv/
COPY --chown=$USER_NAME:$USER_GROUP ROOT/ /

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
# Stage 4:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

ARG USER_UID="65001"
ARG USER_GID="65001"

COPY --from=container-base / /

## DELETE LOG4Shell JNDI LOOKUP CLASS
RUN find /srv -name "*log4j-core-*.jar" -exec zip -q -d {} org/apache/logging/log4j/core/lookup/JndiLookup.class \;



# Switching to non-root user to install SDKMAN! 
USER $USER_UID:$USER_GID
ENV JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk/jre/"
ENV PATH=$PATH:/usr/lib/jvm/java-1.8.0-openjdk/jre/


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
# Expose Elasticsearch Node
EXPOSE 9300