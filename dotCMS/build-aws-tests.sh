#!/usr/bin/env bash

# Enable printing echoing commands
trap 'echo "[$USER@$(hostname) ~]\$ $BASH_COMMAND"' DEBUG

ls -al
