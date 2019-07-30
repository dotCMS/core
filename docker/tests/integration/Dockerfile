# OpenJDK distributed under GPLv2+Oracle Classpath Exception license (http://openjdk.java.net/legal/gplv2+ce.html)
# Alpine Linux packages distributed under various licenses including GPL-3.0+ (https://pkgs.alpinelinux.org/packages)
FROM gcr.io/cicd-246518/tests-seed as prepare-integration

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
ENV BUILD_FROM_ENV=${BUILD_FROM}

#  Value resolved in the context of $BUILD_FROM
ARG BUILD_ID=origin/master
ENV BUILD_ID_ENV=${BUILD_ID}

#  In order to run the integration test a license must be provided
ARG LICENSE_KEY

# Extra parameters like debug options
ARG TESTS_PARAMS
ENV TESTS_PARAMS=${TESTS_PARAMS}

WORKDIR /srv

# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
COPY setup/build-src/checkout.sh /build/checkout.sh
RUN chmod 500 /build/checkout.sh && /build/checkout.sh ${BUILD_FROM} ${BUILD_ID}

COPY setup/build-src/overrideProperties.sh /build/overrideProperties.sh
RUN chmod 500 /build/overrideProperties.sh && /build/overrideProperties.sh

COPY setup/build-src/prepareIntegrationTests.sh /build/prepareIntegrationTests.sh
RUN chmod 500 /build/prepareIntegrationTests.sh && /build/prepareIntegrationTests.sh

COPY setup/build-src/prepareLicense.sh /build/prepareLicense.sh
RUN chmod 500 /build/prepareLicense.sh && /build/prepareLicense.sh ${LICENSE_KEY}

COPY setup/build-src/displayDebugData.sh /build/displayDebugData.sh
RUN chmod 500 /build/displayDebugData.sh && /build/displayDebugData.sh ${BUILD_FROM} ${BUILD_ID}

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