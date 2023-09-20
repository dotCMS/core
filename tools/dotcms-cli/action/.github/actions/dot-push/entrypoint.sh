#!/bin/sh

 ls -la /github/workspace/

  var=$(bash /dot-cli/run-java.sh "$@" )
  #echo "var: $var"
  echo "exit code: $?"
  echo "Quarkus log file"
  cat "${QUARKUS_LOG_FILE_PATH}"