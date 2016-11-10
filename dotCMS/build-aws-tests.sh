#!/usr/bin/env bash

# Enable echoing commands
trap 'echo "[$USER@$(hostname) ~]\$ $BASH_COMMAND"' DEBUG

ls -al
