#!/bin/sh

PRG="$0"
PRGDIR=`dirname "$PRG"`
HAZELCAST_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`/hazelcast
PID_FILE=$HAZELCAST_HOME/hazelcast_instance.pid
PID=$(cat ${PID_FILE});

if [ -z "${PID}" ]; then
    echo "${PID_FILE}.pid is not running (missing PID)."
else
   kill ${PID}
fi