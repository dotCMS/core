FROM amazon/opendistro-for-elasticsearch:0.9.0
WORKDIR /usr/share/elasticsearch/

ARG ES_ADMIN_PASSWORD

COPY ./certs/root-ca.pem /usr/share/elasticsearch/config/root-ca.pem
COPY ./certs/esnode.pem /usr/share/elasticsearch/config/esnode.pem
COPY ./certs/esnode-key.pem /usr/share/elasticsearch/config/esnode-key.pem
COPY ./yml/custom-elasticsearch.yml /usr/share/elasticsearch/config/elasticsearch.yml
COPY ./yml/custom-config.yml /usr/share/elasticsearch/plugins/opendistro_security/securityconfig/config.yml
COPY ./yml/internal_users.yml /usr/share/elasticsearch/plugins/opendistro_security/securityconfig/internal_users.yml
COPY ./init.sh /usr/share/elasticsearch/init.sh

RUN chmod 500 /usr/share/elasticsearch/init.sh
ENV ES_ADMIN_PASSWORD=$ES_ADMIN_PASSWORD
RUN /bin/bash -c "/usr/share/elasticsearch/init.sh"