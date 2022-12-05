#!/bin/bash

set -e

discoverBackends() {

    candidate_ip_list=$( getServiceIpAddresses "$1" )
    candidate_ips=()
    IFS=',' read -ra candidate_ips <<< "${candidate_ip_list}" && unset IFS

    echo -n >/srv/config/backend_members.txt
    if [[ ${#candidate_ips[@]} -gt 0 ]]; then

        for ip in ${candidate_ips[@]}; do
            echo ${ip} >>/srv/config/backend_members.txt
        done

        echo ${#candidate_ips[@]}
    else
        echo 0
    fi

}