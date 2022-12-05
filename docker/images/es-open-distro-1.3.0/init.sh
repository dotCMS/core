#!/bin/bash
set -e

if [[ ! -z "${ES_ADMIN_PASSWORD}" ]]; then
	echo "Setting password for admin user..." 
    cd /usr/share/elasticsearch/plugins/opendistro_security/tools
    chmod 500 /usr/share/elasticsearch/plugins/opendistro_security/tools/hash.sh
    es_hash_password=`(/usr/share/elasticsearch/plugins/opendistro_security/tools/hash.sh -p $ES_ADMIN_PASSWORD)`

    sed -i -e "s|\$2a\$12\$VcCDgh2NDk07JGN0rjGbM.Ad41qVR/YFJcgHp0UGns5JDymv..TOG|${es_hash_password}|" /usr/share/elasticsearch/plugins/opendistro_security/securityconfig/internal_users.yml
fi