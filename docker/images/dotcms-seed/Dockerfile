# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
FROM openjdk:8-jdk-alpine as dotcms-seed

LABEL com.dotcms.contact "info@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"


WORKDIR /srv

# Build env dependencies
RUN apk update && apk --no-cache add bash grep openssl git nodejs=10.14.2-r0 npm=10.14.2-r0  sed


RUN mkdir -p /build/src \
    && echo "Pulling dotCMS src" \
    && cd /build/src && git clone https://github.com/dotCMS/core.git core \ 
    && cd /build/src/core/dotCMS \
    && git gc --aggressive \
    && ./gradlew  --no-daemon  downloadDependencies clonePullTomcatDist 
