FROM haproxy:1.8-alpine

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

RUN rm /docker-entrypoint.sh

WORKDIR /

RUN apk --no-cache upgrade \
    && apk add --no-cache bash openssl ca-certificates gnupg grep tini s6-dns \
    && update-ca-certificates

ENV DOCKERIZE_VERSION v0.6.0
RUN wget --quiet https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz


COPY ROOT/ /
RUN mkdir -p /srv/config

RUN find /srv/ -type f -name "*.sh" -exec chmod 500 {} \; 

RUN chmod -R g=u /srv

ENV SERVICE_DELAY_DEFAULT_MIN 1
ENV SERVICE_DELAY_DEFAULT_STEP 3
ENV SERVICE_DELAY_DEFAULT_MAX 30


EXPOSE 8080
EXPOSE 8443

USER 10001

ENTRYPOINT ["/sbin/tini", "--", "/srv/entrypoint.sh"]
CMD ["haproxy"]
