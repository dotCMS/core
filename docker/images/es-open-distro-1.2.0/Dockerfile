FROM amazon/opendistro-for-elasticsearch:1.2.0
WORKDIR /usr/share/elasticsearch/

ARG ES_ADMIN_PASSWORD

#Copying ES certificates 
COPY ./certs/root-ca.pem /usr/share/elasticsearch/config/root-ca.pem
COPY ./certs/root-ca.key /usr/share/elasticsearch/config/root-ca.key
COPY ./certs/kirk.pem /usr/share/elasticsearch/config/kirk.pem
COPY ./certs/kirk.key /usr/share/elasticsearch/config/kirk.key
COPY ./certs/elasticsearch.pem /usr/share/elasticsearch/config/elasticsearch.pem
COPY ./certs/elasticsearch.key /usr/share/elasticsearch/config/elasticsearch.key

#Copying ES configuration
COPY ./yml/custom-elasticsearch.yml /usr/share/elasticsearch/config/elasticsearch.yml
COPY ./yml/custom-config.yml /usr/share/elasticsearch/plugins/opendistro_security/securityconfig/config.yml
COPY ./yml/internal_users.yml /usr/share/elasticsearch/plugins/opendistro_security/securityconfig/internal_users.yml

#Setting ES admin password as an environment variable
ENV ES_ADMIN_PASSWORD=${ES_ADMIN_PASSWORD:-admin}

#Copying scripts to set ES admin password (init.sh) and initialize open distro security plugin with our custom certificates (init_security_plugin.sh and securityadmin.sh) 
COPY ./init.sh /usr/share/elasticsearch/init.sh
COPY ./init_security_plugin.sh /usr/share/elasticsearch/init_security_plugin.sh
RUN chmod +x /usr/share/elasticsearch/init_security_plugin.sh
RUN chmod +x /usr/share/elasticsearch/plugins/opendistro_security/tools/securityadmin.sh
RUN chmod +x /usr/share/elasticsearch/init.sh

#Adding scripts at the beginning of the image entrypoint
RUN sed -i '3 i\sh /usr/share/elasticsearch/init.sh \& sh /usr/share/elasticsearch/init_security_plugin.sh \&' /usr/local/bin/docker-entrypoint.sh

#Installing net-tools to enable netstat command, which is used by init.sh script
#Provide support to run netstat
RUN yum -y install net-tools