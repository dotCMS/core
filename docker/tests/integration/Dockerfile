# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
FROM jgamba/dotcms-checkout as prepare-integration

LABEL com.dotcms.contact = "support@dotcms.com"
LABEL com.dotcms.vendor = "dotCMS LLC"
LABEL com.dotcms.description = "dotCMS Content Management System"

# Location for assets, indexes and felix
RUN mkdir -p /custom \
    && mkdir -p /custom/assets \
    && mkdir -p /custom/dotsecure \
    && mkdir -p /custom/esdata \
    && mkdir -p /custom/felix \
    && mkdir -p /custom/logs \
    && mkdir -p /custom/reports

#  One of ["COMMIT", "TAG" ]
ARG BUILD_FROM=COMMIT

#  Value resolved in the context of $BUILD_FROM
ARG BUILD_ID=origin/master

#  One of ["postgres", "mysql", "oracle", "mssql"]
ARG DB_TYPE=postgres
ENV DB_TYPE_ENV=${DB_TYPE}

WORKDIR /srv

# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
COPY setup/build-src/checkout.sh /build/checkout.sh
RUN chmod 500 /build/checkout.sh && /build/checkout.sh ${BUILD_FROM} ${BUILD_ID}

COPY setup/build-src/overrideProperties.sh /build/overrideProperties.sh
RUN chmod 500 /build/overrideProperties.sh && /build/overrideProperties.sh

COPY setup/build-src/prepareIntegrationTests.sh /build/prepareIntegrationTests.sh
RUN chmod 500 /build/prepareIntegrationTests.sh && /build/prepareIntegrationTests.sh

COPY setup/build-src/generateLicense.sh /build/generateLicense.sh
RUN chmod 500 /build/generateLicense.sh && /build/generateLicense.sh

# ===========================================
#  Copy our custom data to the source code  #
COPY setup/ROOT/ /build/src/core/dotCMS/src/
# ===========================================

VOLUME /db
VOLUME /custom/reports
VOLUME /custom/logs

## Ports
# Java Debugging - must be configured via CMS_JAVA_OPTS
#EXPOSE 8000

COPY setup/build-src/entrypoint.sh /build/entrypoint.sh
RUN chmod 500 /build/entrypoint.sh

ENTRYPOINT ["/build/entrypoint.sh"]
CMD ["dotcms"]