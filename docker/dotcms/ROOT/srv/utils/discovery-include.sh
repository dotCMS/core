#!/bin/bash

set -e

escapeRegexChars() { 
    # Returns a string with regex control chars escaped
    #  Usage:  escapeRegexChars <string> 
    #  Example:  escapeRegexChars "some.string.with[regex chars]" 
    echo "$1" | \
        sed 's/\./\\\./g' | \
        sed 's/\*/\\\*/g' | \
        sed 's/\^/\\\^/g' | \
        sed 's/\//\\\//g' | \
        sed 's/\[/\\\[/g' | \
        sed 's/\]/\\\]/g' 
}

join() { 
    # Joins an array into a string using a given delimiter
    #  Usage:  join <delim> <array>
    #  Example:  join "," ${my_array[@]}
    local IFS="$1"; shift; echo "$*";
}

inArray() {
    # Search if string is in array. Returns true|false
    #  Usage: inArray <string> <array>
    #  Example: inArray "a string" "${my_array[@]}" 
    local e;
    for e in "${@:2}"; do [[ "$e" == "$1" ]] && echo true; done
    echo false;
}

getRouteAddrToService() {
    # Provides the local interface IP that routes to a given service name.
    local svc_addr_list="$(getServiceIpAddresses $1)"
    local svc_addrs=()
    IFS=',' read -ra svc_addrs <<< "${svc_addr_list}" && unset IFS

    local addr
    local route_addr
    for addr in ${svc_addrs[@]}; do
        if [[ ! "${route_addr}" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
            route_addr=$(ip route get $addr |grep $addr | awk -F " src " '{print$2}' | tr -d [:space:] )
        fi
    done

    if [[ "${route_addr}" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$  ]]; then
        echo "${route_addr}"
    else
        echo ''
    fi
}

getServiceIpAddresses() {
    # Provides a list of IP address when provided a list of hostnames, filters out my addresses when optionally given list of them
    #  Usage: getServiceIpAddresses <service names> <my IP addresses>
    local svc_names=()
    IFS=',' read -ra svc_names <<< "${1}" && unset IFS

    local svc_name
    local query_addrs=()
    local addr
    local svc_addrs=()
    for svc_name in "${svc_names[@]}"; do
        query_addrs=( $(s6-dnsip4 -q ${svc_name} 2>/dev/null || exit 0 |grep -oE "\b([0-9]{1,3}\.){3}[0-9]{1,3}\b" || exit 0) )
        for addr in "${query_addrs[@]}"; do
            if [[ "${addr}" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$  ]]; then
                svc_addrs+=("${addr}")
            fi
        done
    done

    local return_addrs
    if [[ -n "${2}" ]]; then
        local my_addrs=()
        IFS=',' read -ra my_addrs <<< "${2}" && unset IFS

        local addr
        local svc_addrs_filtered=()
        for addr in ${svc_addrs[@]}; do
            if  [[ $(inArray "${addr}" "${my_addrs[@]}") == false  ]]; then
                svc_addrs_filtered+=(${addr})
            fi
        done

        return_addrs=${svc_addrs_filtered[@]}
    else
        return_addrs=${svc_addrs[@]}
    fi

    if [[ ${#return_addrs[@]} -gt 0 ]]; then
        echo $(join "," ${return_addrs[@]})
    else
        echo ''
    fi
}

getMyNetworkInterfaces() {
    # Provides list of network interface devices
    local ifaces=$(ip link | grep -E "^[0-9]+:" | awk '{print$2}' | sed 's/:$//g' | awk -F '@' '{print$1}')

    if [[ ${#ifaces[@]} -gt 0 ]]; then
        echo $(join "," ${ifaces[@]})
    else
        echo ''
    fi
}

getMyIpAddresses() {
    # Provides list of local IP addresses given list of network interface devices
    local iface
    local ifaces=()
    IFS=',' read -ra ifaces <<< "${@}" && unset IFS

    local iface_addrs=()
    for iface in ${ifaces[@]}; do
        local addrs=$(ip addr show dev ${iface} | grep "\ *inet " | awk '{print $2}' | awk -F '/' '{print$1}');
        local addr
        for addr in ${addrs}; do
            iface_addrs+=(${addr})
        done
    done

    if [[ ${#iface_addrs[@]} -gt 0 ]]; then
        echo $(join "," ${iface_addrs[@]})
    else
        echo ''
    fi
}