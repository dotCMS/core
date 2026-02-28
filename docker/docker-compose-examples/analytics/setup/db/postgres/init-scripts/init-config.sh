#!/bin/bash

set -e

echo "Detected MAX_LOCKS_PER_TRANSACTION: '${MAX_LOCKS_PER_TRANSACTION}'"
max_locks=$(echo -e "${MAX_LOCKS_PER_TRANSACTION}" | tr -d '[:space:]')

if [[ "${max_locks}" != '' ]]; then
  sed -i "s,^#max_locks_per_transaction =.*$,max_locks_per_transaction = ${max_locks},g" /var/lib/postgresql/18/data/postgresql.conf \
  cat /var/lib/postgresql/18/data/postgresql.conf | grep 'max_locks_per_transaction'
fi
