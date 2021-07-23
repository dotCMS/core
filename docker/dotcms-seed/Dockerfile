# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
FROM dotcms/java-base as builder

LABEL com.dotcms.contact "info@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

WORKDIR /srv

# Build env dependencies
RUN apt update && \
    apt upgrade && \
	apt install -y --no-install-recommends git

RUN mkdir -p /build/src && cd /build/src && git clone https://github.com/dotCMS/core

#COPY --from=dotcms/dotcms-seed /build/src/core /build/src/core

ENV GRADLE_USER_HOME=/build/src/core/dotCMS/.gradle
ENV GRADLE_OPTS="-Dfile.encoding=utf-8 -Xmx2g"

RUN cd /build/src/core/dotCMS \
    && git pull \
    && ./gradlew  --no-daemon downloadDependencies 


RUN apt purge --allow-remove-essential -y git bash zip unzip && \
    apt autoremove -y && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*