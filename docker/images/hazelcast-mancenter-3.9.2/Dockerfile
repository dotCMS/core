FROM openjdk:8-jre-alpine
ENV MANCENTER_VERSION 3.9.2
ENV MANCENTER_HOME /srv/

EXPOSE 8080

RUN apk --no-cache upgrade
RUN apk add --no-cache tini openssl gnupg bash curl grep bind-tools

WORKDIR $MANCENTER_HOME

RUN wget -O mancenter.tgz https://download.hazelcast.com/management-center/management-center-${MANCENTER_VERSION}.tar.gz && \
    tar -C "${MANCENTER_HOME}" --strip-components=1 -xzf mancenter.tgz && \
    rm mancenter.tgz

ADD docker-entrypoint.sh ./
RUN chmod +x ./docker-entrypoint.sh ./startManCenter.sh

RUN chmod -R g=u ${MANCENTER_HOME}

USER 10001

# Start hazelcast standalone server.
ENTRYPOINT ["/sbin/tini", "-g", "--", "./docker-entrypoint.sh"]
CMD ["mancenter"]
