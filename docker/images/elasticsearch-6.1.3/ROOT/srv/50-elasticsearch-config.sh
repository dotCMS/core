#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh


sleep ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MIN}

count=$(( ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MIN} + RANDOM % ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MAX} ))
echo "Trying discovery for ~${count} seconds"
while [[ $count -ge ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MIN} && ${#es_members[@]} -lt 1 ]]; do

    [[ -n "${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT}" ]] || PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT=$( getRouteAddrToService "${PROVIDER_ELASTICSEARCH_DNSNAMES}" )

    es_candidate_ip_list=$(getServiceIpAddresses "${PROVIDER_ELASTICSEARCH_DNSNAMES}" $( getMyIpAddresses $( getMyNetworkInterfaces ) ) )
    es_candidate_ips=()
    IFS=',' read -ra es_candidate_ips <<< "${es_candidate_ip_list}" && unset IFS

    es_members=()
    if [[ ${#es_candidate_ips[@]} -gt 0 ]]; then
        echo "Testing server candidates: ${es_candidate_ip_list}..."
        for es_candidate_ip in "${es_candidate_ips[@]}"; do
            echo -n "  ${es_candidate_ip}: "
            if dockerize -wait "tcp://${es_candidate_ip}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" -timeout ${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP}s true &> /dev/null; then
                [[ $(inArray "${es_member_ip}" "${es_members[@]}" ) == false ]] && es_members+=(${es_candidate_ip})
                echo "live"
                break 2
            else
                echo "not live"
            fi
            (( count-=${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP} )) || :
        done
    else
        echo "No server candidates found, waiting ${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP} seconds..."
        sleep ${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP}
        (( count-=${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP} )) || :
    fi
done


# Bind address fallback
if [[ ! -n "${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT}" ]]; then
    echo "Service discovery failure, using localhost"
    PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT="127.0.0.1"
fi

[[ -e /srv/config/discovery-file/unicast_hosts.txt  ]] && rm /srv/config/discovery-file/unicast_hosts.txt 
touch /srv/config/discovery-file/unicast_hosts.txt 
    
if [[  ${#es_members[@]} -eq 0 ]]; then
    echo "No live members found, using self (${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT})"
    es_members+=(${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT})
elif [[  ${#es_members[@]} -gt 0 ]]; then
   echo "ES INITIAL DISCOVERY: ${#es_members[@]} members found."
    for es_member in "${es_members[@]}"; do
        echo -e "\n${es_member}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>/srv/config/discovery-file/unicast_hosts.txt
    done
    cat /srv/config/discovery-file/unicast_hosts.txt 
fi


export PROVIDER_ELASTICSEARCH_CLUSTER_NAME
export PROVIDER_ELASTICSEARCH_PORT_TRANSPORT
export PROVIDER_ELASTICSEARCH_PORT_HTTP
export PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT
export PROVIDER_ELASTICSEARCH_ADDR_HTTP
export PROVIDER_ELASTICSEARCH_ENABLE_HTTP

export PROVIDER_ELASTICSEARCH_NODE_MASTER
export PROVIDER_ELASTICSEARCH_NODE_DATA

dockerize -template /srv/templates/elasticsearch.yml.tmpl:/srv/config/elasticsearch.yml
