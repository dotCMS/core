FROM dotcms/java-base:11.0.16-amzn


# Installing basic packages 
RUN apt update && \
    apt upgrade -y && \
    apt install -y --no-install-recommends zip unzip wget libtcnative-1 tzdata tini ca-certificates openssl libapr1 libpq-dev

# ensure elasticsearch user exists
# Defining default non-root user UID, GID, and name 
ARG USER_UID="65005"
ARG USER_GID="65005"
ARG USER_GROUP="elasticsearch"
ARG USER_NAME="elasticsearch"

RUN groupadd -f -g $USER_GID $USER_GROUP
# Creating default non-user
# the useradd
RUN useradd -l -d /srv -g $USER_GID -u $USER_UID $USER_NAME

ENV ELASTICSEARCH_VERSION 6.1.3
ENV ELASTICSEARCH_TARBALL="https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz" 
ENV	ELASTICSEARCH_TARBALL_SHA512="af10cc571ab55f52ab73a86373fa8359214a7866fbb6d2910669e6be897bae30c41c007c3be5803a7f07736041f32cf36425c182a18fab39499321ed9eb4b349"

ENV JAVA_HOME="/java"
ENV PATH /srv/bin:$PATH

WORKDIR /srv

RUN mkdir -p /data
RUN mkdir -p /logs
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
	# we shouldn't need much RAM to test --version (default is 2gb, which gets Jenkins in trouble sometimes)
	export ES_JAVA_OPTS='-Xms512m -Xmx2g'; \
	elasticsearch --version;
	
	
	
COPY ROOT/srv/config/ /srv/config
COPY ROOT/srv/entrypoint.sh /srv/entrypoint.sh
COPY ROOT/srv/plugins /srv/plugins
RUN bin/elasticsearch-plugin install discovery-file


RUN chown -R $USER_NAME:$USER_NAME /srv
RUN chown -R $USER_NAME:$USER_NAME /data
RUN chown -R $USER_NAME:$USER_NAME /logs
RUN find /srv/ -type f -name "*.sh" -exec chmod 500 {} \; 

## make sure the settings stick
RUN sh -c "echo 'vm.max_map_count=262144' >> /etc/sysctl.conf"
RUN sh -c "echo 'fs.file-max=100000' >> /etc/sysctl.conf"
RUN sh -c "echo 'elasticsearch  -  nofile  65535' >> /etc/security/limits.conf"




VOLUME /data
VOLUME /logs

EXPOSE 9200
EXPOSE 9300

USER $USER_UID

ENTRYPOINT ["tini", "--", "/srv/entrypoint.sh"]
