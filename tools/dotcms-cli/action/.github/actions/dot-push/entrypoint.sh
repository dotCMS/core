#!/bin/sh
  echo "Running dot-push action entrypoint.sh"

  bash /dot-cli/run-java.sh "$@"
  exit_code=$?

  echo "exit_code=$exit_code" >> "$GITHUB_OUTPUT"

  echo "Quarkus log file contents:"
  cat "${QUARKUS_LOG_FILE_PATH}"