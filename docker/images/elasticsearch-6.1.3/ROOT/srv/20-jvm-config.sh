#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

export PROVIDER_ELASTICSEARCH_HEAP_SIZE

dockerize -template /srv/templates/jvm.options.tmpl:/srv/config/jvm.options
