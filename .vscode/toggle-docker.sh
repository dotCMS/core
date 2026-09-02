#!/bin/bash

MODE="${1:-normal}"
PORT="${2:-7070}"

case "$MODE" in
  debug-suspend)
    echo "▶️  Starting Docker on port $PORT (debug-suspend, attach debugger to 5005)..."
    ./mvnw -pl :dotcms-core -Pdocker-start,debug-suspend -Dtomcat.port="$PORT"
    ;;
  debug)
    echo "▶️  Starting Docker on port $PORT (debug mode, attach debugger to 5005)..."
    ./mvnw -pl :dotcms-core -Pdocker-start,debug -Dtomcat.port="$PORT"
    ;;
  *)
    echo "▶️  Starting Docker on port $PORT..."
    ./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port="$PORT"
    ;;
esac
