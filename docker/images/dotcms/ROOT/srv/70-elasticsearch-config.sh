#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

echo "Elasticsearch Config ...."
if [[ -z "${PROVIDER_ELASTICSEARCH_DNSNAMES}" ]]; then

    echo "PROVIDER_ELASTICSEARCH_DNSNAMES=" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_CLUSTER_NAME=${PROVIDER_ELASTICSEARCH_CLUSTER_NAME}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT=127.0.0.1" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_PORT_TRANSPORT=${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ADDR_HTTP=${PROVIDER_ELASTICSEARCH_ADDR_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_PORT_HTTP=${PROVIDER_ELASTICSEARCH_PORT_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ENABLE_HTTP=${PROVIDER_ELASTICSEARCH_ENABLE_HTTP}" >>/srv/config/settings.ini

else

    es_discovery_filename="${TOMCAT_HOME}/webapps/ROOT/WEB-INF/elasticsearch/config/discovery-file/unicast_hosts.txt"

    sleep ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MIN}

    count=${PROVIDER_ELASTICSEARCH_SVC_DELAY_MAX}
    echo "Trying discovery for ${count} seconds"
    while [[ $count -ge ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MIN} ]]; do

        [[ -n "${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT}" ]] || PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT=$( getRouteAddrToService "${PROVIDER_ELASTICSEARCH_DNSNAMES}" )

        es_candidate_ip_list=$(getServiceIpAddresses "${PROVIDER_ELASTICSEARCH_DNSNAMES}" $( getMyIpAddresses $( getMyNetworkInterfaces ) ) )
        es_candidate_ips=()
        IFS=',' read -ra es_candidate_ips <<< "${es_candidate_ip_list}" && unset IFS

        es_members=()
        if [[ ${#es_candidate_ips[@]} -gt 0 ]]; then
            echo "Testing server candidates: ${es_candidate_ip_list}..."
            for es_candidate_ip in "${es_candidate_ips[@]}"; do
                echo -n "  ${es_candidate_ip}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}: "
                if dockerize -wait "tcp://${es_candidate_ip}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" -timeout ${PROVIDER_ELASTICSEARCH_SVC_DELAY_STEP}s true 2&>1 >/dev/null; then
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


    if [[ ! -n "${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT}" ]]; then
        echo "Unable to find Elasticsearch transport bind address"
        exit 1
    fi

    [[ -e ${es_discovery_filename}  ]] && rm ${es_discovery_filename}
    touch ${es_discovery_filename} 

    if [[ ${#es_members[@]} -gt 0 ]]; then
        for es_member in "${es_members[@]}"; do
            echo "${es_member}:${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>${es_discovery_filename}
        done
    else
        echo "Unable to find any live Elasticsearch cluster members after ${PROVIDER_ELASTICSEARCH_SVC_DELAY_MAX} seconds"
        exit 1
    fi


    echo "PROVIDER_ELASTICSEARCH_CLUSTER_NAME=${PROVIDER_ELASTICSEARCH_CLUSTER_NAME}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT=${PROVIDER_ELASTICSEARCH_ADDR_TRANSPORT}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_PORT_TRANSPORT=${PROVIDER_ELASTICSEARCH_PORT_TRANSPORT}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ADDR_HTTP=${PROVIDER_ELASTICSEARCH_ADDR_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_PORT_HTTP=${PROVIDER_ELASTICSEARCH_PORT_HTTP}" >>/srv/config/settings.ini
    echo "PROVIDER_ELASTICSEARCH_ENABLE_HTTP=${PROVIDER_ELASTICSEARCH_ENABLE_HTTP}" >>/srv/config/settings.ini
fi
