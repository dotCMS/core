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
    && mkdir -p /custom/output

#  One of ["COMMIT", "TAG" ]
ARG BUILD_FROM=COMMIT
ENV BUILD_FROM=${BUILD_FROM}
ARG BUILD_ID=origin/master
ENV BUILD_ID=${BUILD_ID}
ARG BUILD_HASH
ENV BUILD_HASH=${BUILD_HASH}

#  In order to run the integration test a license must be provided
ARG LICENSE_KEY

WORKDIR /srv

# dotCMS core distributed under GPLv3 license (https://github.com/dotCMS/core/blob/master/license.txt)
COPY setup/build-src/checkout.sh /build/checkout.sh
RUN chmod 500 /build/checkout.sh && /build/checkout.sh ${BUILD_FROM} ${BUILD_ID}

COPY setup/build-src/overrideProperties.sh /build/overrideProperties.sh
RUN chmod 500 /build/overrideProperties.sh && /build/overrideProperties.sh

COPY setup/build-src/prepareTests.sh /build/prepareTests.sh
RUN chmod 500 /build/prepareTests.sh && /build/prepareTests.sh

COPY setup/build-src/prepareLicense.sh /build/prepareLicense.sh
RUN chmod 500 /build/prepareLicense.sh && /build/prepareLicense.sh ${LICENSE_KEY}

COPY setup/build-src/displayDebugData.sh /build/displayDebugData.sh
RUN chmod 500 /build/displayDebugData.sh && /build/displayDebugData.sh ${BUILD_FROM} ${BUILD_ID} ${BUILD_HASH}

# ===========================================
#  Copy our custom data to the source code  #
COPY setup/ROOT /build/src/core/dotCMS/src/
# ===========================================

VOLUME /db
VOLUME /custom/output

COPY setup/build-src/storage.sh /build/storage.sh
RUN chmod 500 /build/storage.sh

COPY setup/build-src/github_status.sh /build/github_status.sh
RUN chmod 500 /build/github_status.sh

COPY setup/build-src/printStatus.sh /build/printStatus.sh
RUN chmod 500 /build/printStatus.sh

COPY setup/build-src/integrationTests.sh /build/integrationTests.sh
RUN chmod 500 /build/integrationTests.sh

COPY setup/build-src/unitTests.sh /build/unitTests.sh
RUN chmod 500 /build/unitTests.sh

COPY setup/build-src/entrypoint.sh /build/entrypoint.sh
RUN chmod 500 /build/entrypoint.sh

ENTRYPOINT ["/build/entrypoint.sh"]
CMD ["integration"]