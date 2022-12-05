#!/usr/bin/with-contenv /bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

if [[ -n "${PROVIDER_HAZELCAST_MANCENTER_URL}" ]]; then
    PROVIDER_HAZELCAST_MANCENTER_ENABLED="true"
else
    PROVIDER_HAZELCAST_MANCENTER_ENABLED="false"
fi

sleep ${PROVIDER_HAZELCAST_SVC_DELAY_MIN}

count=$(( ${PROVIDER_HAZELCAST_SVC_DELAY_MIN} + RANDOM % ${PROVIDER_HAZELCAST_SVC_DELAY_MAX} ))
echo "Trying discovery for ~${count} seconds"
while [[ $count -ge ${PROVIDER_HAZELCAST_SVC_DELAY_MIN} ]]; do

    [[ -n "${PROVIDER_HAZELCAST_BIND_ADDR}" ]] || PROVIDER_HAZELCAST_BIND_ADDR=$( getRouteAddrToService "${PROVIDER_HAZELCAST_DNSNAMES}" )

    hz_candidate_ip_list=$( getServiceIpAddresses "${PROVIDER_HAZELCAST_DNSNAMES}" $( getMyIpAddresses $( getMyNetworkInterfaces ) ) )
    hz_candidate_ips=()
    IFS=',' read -ra hz_candidate_ips <<< "${hz_candidate_ip_list}" && unset IFS

    if [[ ${#hz_candidate_ips[@]} -gt 0 ]]; then
        for hz_candidate_ip in "${hz_candidate_ips[@]}"; do

            hz_member_ips=($(wget --quiet -O - -T ${PROVIDER_HAZELCAST_SVC_DELAY_STEP} ${hz_candidate_ip}:${PROVIDER_HAZELCAST_PORT}/hazelcast/rest/cluster 2>/dev/null | grep -oE "\b([0-9]{1,3}\.){3}[0-9]{1,3}\b" || : ) )
            if [[ ${#hz_member_ips[@]} -lt 1 ]]; then
                echo "No members found from candidate ${hz_candidate_ip}";  
            else
                for hz_member_ip in "${hz_member_ips[@]}"; do
                    if [[ $(inArray "${hz_member_ip}" "${hz_members[@]}" ) == false ]]; then
                        echo "Found member ${hz_member_ip}"
                        hz_members+=(${hz_member_ip})
                    fi
                done
            fi
            sleep ${PROVIDER_HAZELCAST_SVC_DELAY_STEP};
            (( count-=${PROVIDER_HAZELCAST_SVC_DELAY_STEP} )) || :
        done
    else
        echo "No candidate members found, waiting ${PROVIDER_HAZELCAST_SVC_DELAY_STEP} seconds..."
        sleep ${PROVIDER_HAZELCAST_SVC_DELAY_STEP}
        (( count-=${PROVIDER_HAZELCAST_SVC_DELAY_STEP} )) || :
    fi
done


# Bind address fallback
if [[ ! -n "${PROVIDER_HAZELCAST_BIND_ADDR}" ]]; then
    echo "Service discovery failure, using localhost"
    PROVIDER_HAZELCAST_BIND_ADDR="127.0.0.1"
fi

if [[  ${#hz_members[@]} -eq 0 ]]; then
    echo "No live members found, using self (${PROVIDER_HAZELCAST_BIND_ADDR})"
    hz_members+=(${PROVIDER_HAZELCAST_BIND_ADDR})
fi

echo "PROVIDER_HAZELCAST_GROUPNAME=${PROVIDER_HAZELCAST_GROUPNAME}" >> /srv/config.ini
echo "PROVIDER_HAZELCAST_PORT=${PROVIDER_HAZELCAST_PORT}" >> /srv/config.ini
echo "PROVIDER_HAZELCAST_BIND_ADDR=${PROVIDER_HAZELCAST_BIND_ADDR}" >> /srv/config.ini
echo "PROVIDER_HAZELCAST_MANCENTER_ENABLED=${PROVIDER_HAZELCAST_MANCENTER_ENABLED}" >> /srv/config.ini
echo "PROVIDER_HAZELCAST_MANCENTER_URL=${PROVIDER_HAZELCAST_MANCENTER_URL}" >> /srv/config.ini
echo "PROVIDER_HAZELCAST_DISCOVERY_MEMBERS=$(join , ${hz_members[@]})" >> /srv/config.ini



