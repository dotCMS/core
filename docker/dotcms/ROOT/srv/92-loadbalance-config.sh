#!/bin/bash

set -e

source /srv/utils/discovery-include.sh
source /srv/utils/config-defaults.sh

echo "Haproxy config ...."
if [[ -z "${PROVIDER_LOADBALANCER_DNSNAMES}" ]]; then

    echo "CMS_VALVE_IP=${CMS_VALVE_IP}" >>/srv/config/settings.ini

else
	sleep ${PROVIDER_LOADBALANCER_SVC_DELAY_MIN}

	count=${PROVIDER_LOADBALANCER_SVC_DELAY_MAX}
	echo "Trying discovery for ${count} seconds"

	haproxy_ip_list=$( getServiceIpAddresses "$PROVIDER_LOADBALANCER_DNSNAMES" )
	while [[ $count -ge ${PROVIDER_LOADBALANCER_SVC_DELAY_MIN} ]]; do
		for haproxy_candidate_ip in "${haproxy_ip_list[@]}"; do
			if dockerize -wait "tcp://${haproxy_candidate_ip}:${PROVIDER_LOADBALANCER_PORT_HTTP}" -timeout ${PROVIDER_LOADBALANCER_SVC_DELAY_STEP}s true 2&>1 >/dev/null; then
				echo "${PROVIDER_LOADBALANCER_DNSNAMES} live at IP ${haproxy_candidate_ip}"
				: ${CMS_VALVE_IP:="%a %A"}
				echo "CMS_VALVE_IP=${CMS_VALVE_IP}" >>/srv/config/settings.ini
	            break 2
			else 
				echo "${PROVIDER_LOADBALANCER_DNSNAMES} not live"
			fi	
		done	
		sleep ${PROVIDER_LOADBALANCER_SVC_DELAY_STEP}
    done
	echo "Detected ${PROVIDER_LOADBALANCER_DNSNAMES}"
fi