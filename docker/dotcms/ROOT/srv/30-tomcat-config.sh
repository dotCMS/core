#!/bin/bash

set -e

source /srv/utils/config-defaults.sh

echo "Tomcat config ...."
echo "CMS_CONNECTOR_THREADS=${CMS_CONNECTOR_THREADS}" >>/srv/config/settings.ini
echo "CMS_SMTP_HOST=${CMS_SMTP_HOST}" >>/srv/config/settings.ini

echo "CMS_COMPRESSION=${CMS_COMPRESSION}" >>/srv/config/settings.ini
echo "CMS_COMPRESSIBLEMIMETYPE=${CMS_COMPRESSIBLEMIMETYPE}" >>/srv/config/settings.ini

echo "CMS_ACCESSLOG_PATTERN=${CMS_ACCESSLOG_PATTERN}" >>/srv/config/settings.ini
echo "CMS_ACCESSLOG_FILEDATEFORMAT=${CMS_ACCESSLOG_FILEDATEFORMAT}" >>/srv/config/settings.ini
echo "CMS_ACCESSLOG_MAXDAYS=${CMS_ACCESSLOG_MAXDAYS}" >>/srv/config/settings.ini

echo "CMS_REMOTEIP_REMOTEIPHEADER=${CMS_REMOTEIP_REMOTEIPHEADER}" >>/srv/config/settings.ini
echo "CMS_REMOTEIP_INTERNALPROXIES=${CMS_REMOTEIP_INTERNALPROXIES}" >>/srv/config/settings.ini

