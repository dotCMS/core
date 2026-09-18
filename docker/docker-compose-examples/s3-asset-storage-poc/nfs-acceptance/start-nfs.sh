#!/bin/sh
set -eu
mount -t nfsd nfsd /proc/fs/nfsd
rpcbind -w
exportfs -rav
rpc.nfsd -N 3 -U --grace-time 10 --lease-time 10 4
rpc.mountd -N 3 -F &
mountd_pid=$!
cleanup() {
    trap - EXIT TERM INT
    rpc.nfsd 0
    exportfs -au
    kill "$mountd_pid" 2>/dev/null || true
    wait "$mountd_pid" 2>/dev/null || true
}
trap cleanup EXIT TERM INT
wait "$mountd_pid"
