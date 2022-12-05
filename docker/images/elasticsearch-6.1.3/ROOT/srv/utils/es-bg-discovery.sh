#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

sleep $PROVIDER_ELASTICSEARCH_SVC_REFRESH
export PROVIDER_ELASTICSEARCH_PORT_TRANSPORT

while [[ ! -f /srv/pid ]]; do
    echo "Elasticsearch background discovery: pidfile not found, waiting ${PROVIDER_ELASTICSEARCH_SVC_REFRESH} seconds..."
    sleep ${PROVIDER_ELASTICSEARCH_SVC_REFRESH}
done

if [[ -e /srv/INIT_STARTED_ELASTICSEARCH ]]; then
    # Loop as long as Elasticsearch is running
    while kill -s 0 $(cat /srv/pid) 2>/dev/null; do

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

        [[ -e /srv/config/discovery-file/unicast_hosts.txt.new ]] && rm /srv/config/discovery-file/unicast_hosts.txt.new
        touch /srv/config/discovery-file/unicast_hosts.txt.new
        for es_member in "${es_members[@]}"; do
            echo "${es_member}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>/srv/config/discovery-file/unicast_hosts.txt.new
        done

        hash_orig=$(cat /srv/config/discovery-file/unicast_hosts.txt |sort -u |md5sum |cut -f 1 -d ' ')
        hash_new=$(cat /srv/config/discovery-file/unicast_hosts.txt.new |sort -u |md5sum |cut -f 1 -d ' ')

        if [[ "${hash_orig}" != "${hash_new}" ]]; then
            echo "ES DISCOVERY MEMBERS CHANGED: ${#es_members[@]} members"
            cat /srv/config/discovery-file/unicast_hosts.txt.new
            mv /srv/config/discovery-file/unicast_hosts.txt.new /srv/config/discovery-file/unicast_hosts.txt
        fi

        sleep ${PROVIDER_ELASTICSEARCH_SVC_REFRESH}
        
    done

    echo "ERROR: Elasticsearch pid not found!! Quitting"
    s6-svscanctl -t /var/run/s6/services

else

    echo "INIT did not start Elasticsearch, es-bg-discovery skipped"
    while true; do
        sleep 86400
    done

fi
