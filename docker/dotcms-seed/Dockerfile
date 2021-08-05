# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
FROM adoptopenjdk/openjdk11:ubuntu as dotcms-seed

LABEL com.dotcms.contact "info@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

WORKDIR /srv

# Export gradle environment

ENV GRADLE_USER_HOME=/build/src/core/dotCMS/.gradle
ENV GRADLE_OPTS="-Dfile.encoding=utf-8 -Xmx2g"

# Build env dependencies
# Installing basic packages 
RUN apt update && \
    apt upgrade -y && \
    apt install -y bash grep openssl git sed

RUN mkdir -p /build/src \
    && echo "Pulling dotCMS src" \
    && cd /build/src && git clone https://github.com/dotCMS/core.git core \
    && cd /build/src/core/dotCMS \
    && git gc --aggressive \
    && ./gradlew  --no-daemon downloadDependencies clonePullTomcatDist -PuseGradleNode=false

