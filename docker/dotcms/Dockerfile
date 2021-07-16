# ----------------------------------------------
# Stage 1:  Minimal java image with sdkman + Ubuntu LTS
# ----------------------------------------------
FROM ubuntu:20.04 as minimal-java

WORKDIR /srv

# Defining default Java, can be any java provided by sdkman
ARG JAVA_VERSION="21.1.0.r11-grl"

ENV JAVA_OUTPUT_DIR="/java"
ENV DEBIAN_FRONTEND=noninteractive

# Installing basic packages 
RUN apt update && \
	apt install -y --no-install-recommends zip unzip wget bash ca-certificates

# Install curl
RUN wget -O - https://repo.dotcms.com/artifactory/ext-release-local/com/dotcms/curl-static/curl-`uname -m` | install /dev/stdin /usr/bin/curl 
RUN chmod a+x /usr/bin/curl 

# Downloading sdkman
RUN curl -sL "https://get.sdkman.io" | bash

# Installing Java via sdkman 
RUN bash -c "source /root/.sdkman/bin/sdkman-init.sh && \
    yes | sdk install java $JAVA_VERSION "


RUN /root/.sdkman/candidates/java/$JAVA_VERSION/bin/jlink \
    --verbose \
    --add-modules \
        java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported,java.scripting,java.rmi \
        # java.naming - javax/naming/NamingException
        # java.desktop - java/beans/PropertyEditorSupport
        # java.management - javax/management/MBeanServer
        # java.security.jgss - org/ietf/jgss/GSSException
        # java.instrument - java/lang/instrument/IllegalClassFormatException
        # jdk.unsupported - sun/misc/Unsafe, needed by caffine
        # java.scripting - Nashhorn, needed by log4j
        # java.rmi needed by Quartz
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output "$JAVA_OUTPUT_DIR"

ENV PATH="$PATH:/java/bin"

RUN rm -rf /root/.sdkman
RUN apt purge -y zip unzip wget 
RUN apt autoremove -y
RUN apt clean
RUN rm -rf /var/lib/apt/lists/*


# ----------------------------------------------
# Stage 2:  Build dotCMS  
# ----------------------------------------------
FROM dotcms/dotcms-seed:jdk11 as build-cms

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

#  One of ["TARBALL_URL", "RELEASE", "NIGHTLY", "COMMIT", "TAG" ]
ARG BUILD_FROM=COMMIT

#  Value resolved in the context of $BUILD_FROM
ARG BUILD_ID=HEAD

WORKDIR /srv

# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
COPY build-src/ /build/
RUN chmod 500 /build/build_dotcms.sh && /build/build_dotcms.sh ${BUILD_FROM} ${BUILD_ID}

RUN mkdir -p /srv/utils /srv/templates /srv/config /srv/home
RUN chmod -R 666 /srv && find /srv/ -type d -exec chmod a+x {} \;



# ----------------------------------------------
# Stage 3:  Build our container using the minimal-java image
#           and copying the prebuilt dotcms
# ----------------------------------------------
FROM minimal-java as builder

WORKDIR /srv

ENV DEBIAN_FRONTEND=noninteractive

# Installing basic packages 
RUN apt update && \
	apt install -y --no-install-recommends libtcnative-1 tzdata tini ca-certificates openssl

# Install prebuilt dockerize
RUN curl -Ls https://repo.dotcms.com/artifactory/ext-release-local/com/dotcms/dockerize/dockerize-linux-`uname -m` | install /dev/stdin /usr/local/bin/dockerize 

RUN apt purge --allow-remove-essential -y wget
RUN apt autoremove -y
RUN apt clean
RUN rm -rf /var/lib/apt/lists/*
COPY --from=build-cms /srv/ /srv/
COPY ROOT/ /
RUN rm -rf /srv/dotserver/tomcat-9.0.41/.git

# Defining default non-root user UID, GID, and name 
ARG USER_UID="1313"
ARG USER_GID="1313"
ARG USER_NAME="dotcms"

# Creating default non-user 
RUN groupadd -g $USER_GID $USER_NAME && \
	useradd -d /srv -m -g $USER_GID -u $USER_UID $USER_NAME

# Make scripts runable
RUN find /srv/ -type f -name "*.sh" -exec chmod a+x {} \;

# Make plugin merging directories writable
RUN find /srv/templates -type d -exec chmod 770 {} \;

# Make dotcms user owner
RUN mkdir -p /data/shared/assets && \
    mkdir -p /data/local/dotsecure/license && \
    chown -R dotcms /data && \
    find /srv/ -type d -exec chown -R dotcms {} \;

ENV SERVICE_DELAY_DEFAULT_MIN 1
ENV SERVICE_DELAY_DEFAULT_STEP 3
ENV SERVICE_DELAY_DEFAULT_MAX 30

# ----------------------------------------------
# Stage 4:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch

ARG USER_UID="1313"
ARG USER_GID="1313"

COPY --from=builder / /

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