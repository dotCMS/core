#!/bin/bash

set -e

[[ -n "${MAX_LOCKS_PER_TRANSACTION}" ]] \
  && sed -i "s,^#max_locks_per_transaction =.*$,max_locks_per_transaction = ${MAX_LOCKS_PER_TRANSACTION},g" /var/lib/postgresql/data/postgresql.conf \
  && cat /var/lib/postgresql/data/postgresql.conf | grep 'max_locks_per_transaction'
