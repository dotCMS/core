#!/bin/bash
PID=0
sigterm_handler() {
  echo "Hazelcast Term Handler received shutdown signal. Signaling hazelcast instance on PID: ${PID}"
  if [ ${PID} -ne 0 ]; then
    kill "${PID}"
  fi
}

PID_FILE=$HZ_HOME/hazelcast_instance.pid

if [ "x$MIN_HEAP_SIZE" != "x" ]; then
	JAVA_OPTS="$JAVA_OPTS -Xms${MIN_HEAP_SIZE}"
fi

if [ "x$MAX_HEAP_SIZE" != "x" ]; then
	JAVA_OPTS="$JAVA_OPTS -Xmx${MAX_HEAP_SIZE}"
fi

# if we receive SIGTERM (from docker stop) or SIGINT (ctrl+c if not running as daemon)
# trap the signal and delegate to sigterm_handler function, which will notify hazelcast instance process
trap sigterm_handler SIGTERM SIGINT

export CLASSPATH=$HZ_HOME/hazelcast-all-$HZ_VERSION.jar:$HZ_HOME/cache-api-1.0.0.jar:$CLASSPATH/*

echo "########################################"
echo "# RUN_JAVA=$RUN_JAVA"
echo "# JAVA_OPTS=$JAVA_OPTS"
echo "# CLASSPATH=$CLASSPATH"
echo "# starting now...."
echo "########################################"

java -server $JAVA_OPTS com.hazelcast.core.server.StartServer &
PID="$!"
echo "Process id ${PID} for hazelcast instance is written to location: " $PID_FILE
echo ${PID} > ${PID_FILE}

# wait on hazelcast instance process
wait ${PID}
# if a signal came up, remove previous traps on signals and wait again (noop if process stopped already)
trap - SIGTERM SIGINT
wait ${PID}
