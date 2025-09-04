# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
FROM dotcms/fedora-java-base:8-shenandoah

LABEL com.dotcms.contact "info@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

WORKDIR /srv

# Export gradle environment

ENV GRADLE_USER_HOME=/build/src/core/dotCMS/.gradle
ENV GRADLE_OPTS="-Dfile.encoding=utf-8 -Xmx2g"

# Build env dependencies
# Installing basic packages 
RUN dnf update -y
RUN dnf install grep openssl git sed -y --setopt=install_weak_deps=False
    
RUN mkdir -p /build/src \
    && echo "Pulling dotCMS src" \
    && cd /build/src && git clone https://github.com/dotCMS/core.git core 