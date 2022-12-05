FROM openjdk:8-jdk-alpine as build-plugins

RUN apk --no-cache upgrade
RUN apk add --no-cache openssl gnupg bash curl grep git nss

RUN mkdir -p /build/plugins/output
WORKDIR /build

COPY plugins/ /build/plugins/src

RUN set -e && \
	for plugin in $( find /build/plugins/src -mindepth 1 -maxdepth 1 -type d -exec basename {} \; ) ; do \
	    cd "/build/plugins/src/${plugin}" && \
		echo "Building plugin \"${plugin}\"..." && \
		./gradlew jar && \
		echo "Plugin classes:" && \
		jar tf build/libs/*.jar && \
		mkdir -p "/build/plugins/output/${plugin}" && \
		cp build/libs/*.jar plugin-descriptor.properties "/build/plugins/output/${plugin}" && \
		cd /build ; \
	done


FROM openjdk:8-jre-alpine


RUN apk --no-cache upgrade \
	&& apk add --no-cache bash openssl ca-certificates gnupg grep nss tini s6-dns \
	&& update-ca-certificates

ENV DOCKERIZE_VERSION v0.6.0
RUN wget --quiet https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz

# ensure elasticsearch user exists
RUN addgroup -S -g 1000 elasticsearch && adduser -u 1000 -SH -G elasticsearch -h /srv elasticsearch

ENV ELASTICSEARCH_VERSION 6.1.3
ENV ELASTICSEARCH_TARBALL="https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz" 
ENV	ELASTICSEARCH_TARBALL_SHA512="af10cc571ab55f52ab73a86373fa8359214a7866fbb6d2910669e6be897bae30c41c007c3be5803a7f07736041f32cf36425c182a18fab39499321ed9eb4b349"

ENV PATH /srv/bin:$PATH

WORKDIR /srv

RUN set -ex; \
	\
	wget --quiet -O elasticsearch.tar.gz "$ELASTICSEARCH_TARBALL"; \
	\
	if [ "${ELASTICSEARCH_TARBALL_SHA512}" ]; then \
		echo "${ELASTICSEARCH_TARBALL_SHA512}  elasticsearch.tar.gz" | sha512sum -c -; \
	fi; \
	\
	tar -xf elasticsearch.tar.gz --strip-components=1; \
	rm elasticsearch.tar.gz; \
	rm /srv/config/jvm.options; \
	rm /srv/config/elasticsearch.yml; \
	\
	# we shouldn't need much RAM to test --version (default is 2gb, which gets Jenkins in trouble sometimes)
	export ES_JAVA_OPTS='-Xms32m -Xmx32m'; \
	elasticsearch --version;

# Install File-Based Discovery Plugin
RUN bin/elasticsearch-plugin install discovery-file

# Install dotCMS Eleasticsearch plugin
COPY --from=build-plugins /build/plugins/output/ plugins/

COPY ROOT/ /
RUN find /srv/ -type f -name "*.sh" -exec chmod 500 {} \; 

RUN chmod -R g=u /srv

ENV SERVICE_DELAY_DEFAULT_MIN 1
ENV SERVICE_DELAY_DEFAULT_STEP 3
ENV SERVICE_DELAY_DEFAULT_MAX 30

RUN chmod -R g=u /data
RUN chmod -R g=u /logs
VOLUME /data
VOLUME /logs

EXPOSE 9200 9300

USER 10001

ENTRYPOINT ["/sbin/tini", "--", "/srv/entrypoint.sh"]
CMD ["elasticsearch"]
