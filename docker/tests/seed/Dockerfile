# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
FROM openjdk:8-jdk-alpine as dotcms-checkout

LABEL com.dotcms.contact = "info@dotcms.com"
LABEL com.dotcms.vendor = "dotCMS LLC"
LABEL com.dotcms.description = "dotCMS Content Management System"

WORKDIR /srv

# Build env dependencies
RUN apk update && apk --no-cache add bash grep git nodejs-current nodejs-current-dev sed curl coreutils

# Cloning the repository
RUN mkdir -p /build/src \
    && echo "Pulling dotCMS src" \
    && cd /build/src && git clone https://github.com/dotCMS/core.git core \
    && cd /build/src/core/dotCMS \
    && ./gradlew  --no-daemon  downloadDependencies