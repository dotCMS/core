#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

echo "Tomcat config ...."
echo "CMS_CONNECTOR_THREADS=${CMS_CONNECTOR_THREADS}" >>/srv/config/settings.ini
echo "CMS_SMTP_HOST=${CMS_SMTP_HOST}" >>/srv/config/settings.ini