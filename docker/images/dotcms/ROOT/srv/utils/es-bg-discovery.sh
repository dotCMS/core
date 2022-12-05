#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

es_discovery_filename="/srv/dotserver/tomcat-$(cat /srv/TOMCAT_VERSION)/webapps/ROOT/WEB-INF/elasticsearch/config/discovery-file/unicast_hosts.txt"

sleep $PROVIDER_ELASTICSEARCH_SVC_REFRESH

while true; do

    es_candidate_ip_list=$(getServiceIpAddresses "${PROVIDER_ELASTICSEARCH_DNSNAMES}" $( getMyIpAddresses $( getMyNetworkInterfaces ) ) )
    es_candidate_ips=()
    IFS=',' read -ra es_candidate_ips <<< "${es_candidate_ip_list}" && unset IFS

    es_members=()
    if [[ ${#es_candidate_ips[@]} -gt 0 ]]; then
        for es_candidate_ip in "${es_candidate_ips[@]}"; do
            if dockerize -wait "tcp://${es_candidate_ip}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" -timeout ${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP}s true &> /dev/null; then
                [[ $(inArray "${es_member_ip}" "${es_members[@]}" ) == false ]] && es_members+=(${es_candidate_ip})
            fi
            (( count-=${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP} )) || :
        done
    fi

    [[ -e ${es_discovery_filename}.new ]] && rm ${es_discovery_filename}.new
    touch ${es_discovery_filename}.new
    for es_member in "${es_members[@]}"; do
        echo "${es_member}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>${es_discovery_filename}.new
    done

    hash_orig=$(cat ${es_discovery_filename} |sort -u |md5sum |cut -f 1 -d ' ')
    hash_new=$(cat ${es_discovery_filename}.new |sort -u |md5sum |cut -f 1 -d ' ')

    if [[ "${hash_orig}" != "${hash_new}" ]]; then
        echo "ES DISCOVERY MEMBERS CHANGED: ${#es_members[@]} members"
        cat ${es_discovery_filename}.new
        mv ${es_discovery_filename}.new ${es_discovery_filename}
    fi

    sleep $PROVIDER_ELASTICSEARCH_SVC_REFRESH
    
done
