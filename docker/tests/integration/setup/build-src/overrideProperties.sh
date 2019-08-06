#!/bin/bash

assetsFolder="/custom/assets"
dotsecureFolder="/custom/dotsecure"
esdataFolder="/custom/esdata"
logsFolder="/custom/output/logs"
felixFolder="/custom/felix"
dotCMSFolder="/build/src/core/dotCMS"
resourcesFolder="$dotCMSFolder/src/integration-test/resources"

## ------------------
# /build/src/core/dotCMS/gradle.properties
sed -i "s,^# integrationTestFelixFolder=.*$,integrationTestFelixFolder=$felixFolder,g" $dotCMSFolder/gradle.properties

## ------------------
# /build/src/core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties
sed -i "s,^es.path.home=.*$,es.path.home=$dotCMSFolder/src/main/webapp/WEB-INF/elasticsearch,g" $resourcesFolder/it-dotcms-config-cluster.properties

## ------------------
# /build/src/core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties
sed -i "s,^DYNAMIC_CONTENT_PATH=.*$,DYNAMIC_CONTENT_PATH=$dotsecureFolder,g" $resourcesFolder/it-dotmarketing-config.properties
sed -i "s,^TAIL_LOG_LOG_FOLDER=.*$,TAIL_LOG_LOG_FOLDER=$dotsecureFolder/logs/,g" $resourcesFolder/it-dotmarketing-config.properties
sed -i "s,^ASSET_REAL_PATH =.*$,ASSET_REAL_PATH=$assetsFolder,g" $resourcesFolder/it-dotmarketing-config.properties
sed -i "s,^#TOOLBOX_MANAGER_PATH=.*$,TOOLBOX_MANAGER_PATH=$dotCMSFolder/src/main/webapp/WEB-INF/toolbox.xml,g" $resourcesFolder/it-dotmarketing-config.properties
sed -i "s,^VELOCITY_ROOT =.*$,VELOCITY_ROOT=$dotCMSFolder/src/main/webapp/WEB-INF/velocity,g" $resourcesFolder/it-dotmarketing-config.properties
sed -i "s,^GEOIP2_CITY_DATABASE_PATH_OVERRIDE=.*$,GEOIP2_CITY_DATABASE_PATH_OVERRIDE=$dotCMSFolder/src/main/webapp/WEB-INF/geoip2/GeoLite2-City.mmdb,g" $resourcesFolder/it-dotmarketing-config.properties
sed -i "s,^felix.base.dir=.*$,felix.base.dir=$felixFolder,g" $resourcesFolder/it-dotmarketing-config.properties
echo "
felix.felix.fileinstall.dir=$felixFolder/load
" >> $resourcesFolder/it-dotmarketing-config.properties
echo "
felix.felix.undeployed.dir=$felixFolder/undeploy
" >> $resourcesFolder/it-dotmarketing-config.properties
echo "
dotcms.concurrent.locks.disable=false
" >> $resourcesFolder/it-dotmarketing-config.properties

## ------------------
# /build/src/core/dotCMS/src/main/webapp/WEB-INF/elasticsearch/config/elasticsearch-override.yml
echo "
cluster.name: dotCMSContentIndex_docker
path.data: $esdataFolder
path.repo: $esdataFolder/essnapshot/snapshots
path.logs: $logsFolder
# http.port: 9200
# transport.tcp.port: 9309

http.enabled: false
http.cors.enabled: false

# http.host: localhost

cluster.routing.allocation.disk.threshold_enabled: false
" >> $dotCMSFolder/src/main/webapp/WEB-INF/elasticsearch/config/elasticsearch-override.yml
