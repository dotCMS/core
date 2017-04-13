#!/usr/bin/env bash


# Enable echoing commands
trap 'echo "[$USER@$(hostname) $PWD]\$ $BASH_COMMAND"' DEBUG


# Create working directory
mkdir repo
cd repo


# Check out branch under working directory
git clone -b $BRANCH https://github.com/dotCMS/core.git
if [ -n "$COMMIT" ]; then
	cd core
	git checkout $COMMIT
	cd ..
fi


# Build tests and distro
cd core/dotCMS
./gradlew clean --refresh-dependencies
./gradlew copyTestRuntimeLibs individualTestJar integrationTestJar functionalTestJar
./gradlew createDist
cd ../..


# Uncompress distro and tomcat under working directory
tar zxf core/dist-output/dotcms_*.tar.gz
mv dotserver/`ls dotserver | grep  tomcat` dotserver/tomcat


# Copy test JARs into distro's tomcat
cp core/dotCMS/build/libs/dotcms_*-*Test.jar dotserver/tomcat/webapps/ROOT/WEB-INF/lib
cp core/dotCMS/build/libs/test/junit-*.jar dotserver/tomcat/webapps/ROOT/WEB-INF/lib

# Uncompress ant/configuration files for tests
jar xf dotserver/tomcat/webapps/ROOT/WEB-INF/lib/dotcms_*-functionalTest.jar build-tests.xml
jar xf dotserver/tomcat/webapps/ROOT/WEB-INF/lib/dotcms_*-functionalTest.jar context.xml
mv context.xml dotserver/tomcat/webapps/ROOT/META-INF/context.xml


# Setup configuration files
sed -i "s,{driver},$DB_DRIVER,g" dotserver/tomcat/webapps/ROOT/META-INF/context.xml
sed -i "s,{url},$DB_URL,g" dotserver/tomcat/webapps/ROOT/META-INF/context.xml
sed -i "s,{username},$DB_USERNAME,g" dotserver/tomcat/webapps/ROOT/META-INF/context.xml
sed -i "s,{password},$DB_PASSWORD,g" dotserver/tomcat/webapps/ROOT/META-INF/context.xml
sed -i "s,{valquery},$DB_VALIDATION_QUERY,g" dotserver/tomcat/webapps/ROOT/META-INF/context.xml

sed -i "s,dotCMSContentIndex,$ESCLUSTER,g" dotserver/tomcat/webapps/ROOT/WEB-INF/classes/dotcms-config-cluster.properties
sed -i "s,CLUSTER_AUTOWIRE=true,CLUSTER_AUTOWIRE=false,g" dotserver/tomcat/webapps/ROOT/WEB-INF/classes/dotcms-config-cluster.properties

sed -i "s,PUBLISHER_QUEUE_MAX_TRIES=3,PUBLISHER_QUEUE_MAX_TRIES=1,g" dotserver/tomcat/webapps/ROOT/WEB-INF/classes/dotmarketing-config.properties

sed -i "s,^$DB_TYPE.db.driver=.*$,$DB_TYPE.db.driver=$DB_DRIVER,g" core/dotCMS/src/integration-test/resources/db-config.properties
sed -i "s,^$DB_TYPE.db.base.url=.*$,$DB_TYPE.db.base.url=$DB_URL,g" core/dotCMS/src/integration-test/resources/db-config.properties
sed -i "s,^$DB_TYPE.db.username=.*$,$DB_TYPE.db.username=$DB_USERNAME,g" core/dotCMS/src/integration-test/resources/db-config.properties
sed -i "s,^$DB_TYPE.db.password=.*$,$DB_TYPE.db.password=$DB_PASSWORD,g" core/dotCMS/src/integration-test/resources/db-config.properties

sed -i "s,^es.cluster.name *=.*$,es.cluster.name=$ESCLUSTER_3x,g" core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties
sed -i "s,^es.path.data *=.*$,es.path.data=$PWD/dotserver/tomcat/webapps/ROOT/dotsecure/esdata,g" core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties
sed -i "s,^es.path.work *=.*$,es.path.work=$PWD/dotserver/tomcat/webapps/ROOT/dotsecure/esdata/work,g" core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties
sed -i "s,^es.path.repo *=.*$,es.path.repo=$PWD/dotserver/tomcat/webapps/ROOT/dotsecure/esdata/essnapshot/snaphosts,g" core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties
sed -i "s,^es.path.logs *=.*$,es.path.logs=$PWD/dotserver/tomcat/webapps/ROOT/dotsecure/logs,g" core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties
echo "
CLUSTER_AUTOWIRE=false
" >> core/dotCMS/src/integration-test/resources/it-dotcms-config-cluster.properties

sed -i "s,^ASSET_REAL_PATH *=.*$,ASSET_REAL_PATH=$PWD/dotserver/tomcat/webapps/ROOT/assets,g" core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties
sed -i "s,^DYNAMIC_CONTENT_PATH *=.*$,DYNAMIC_CONTENT_PATH=$PWD/dotserver/tomcat/webapps/ROOT/dotsecure,g" core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties
sed -i "s,^VELOCITY_ROOT *=.*$,VELOCITY_ROOT=$PWD/dotserver/tomcat/webapps/ROOT/WEB-INF/velocity,g" core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties
echo "
GEOIP2_CITY_DATABASE_PATH_OVERRIDE=$PWD/dotserver/tomcat/webapps/ROOT/WEB-INF/geoip2/GeoLite2-City.mmdb
" >> core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties
echo "
TOOLBOX_MANAGER_PATH=$PWD/dotserver/tomcat/webapps/ROOT/WEB-INF/toolbox.xml
" >> core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties
echo "
context.path.felix=$PWD/dotserver/tomcat/webapps/ROOT/WEB-INF/felix
" >> core/dotCMS/src/integration-test/resources/it-dotmarketing-config.properties


# Create output directory
mkdir tests


# Run End-2-End tests
ant -f build-tests.xml test-dotcms
cp dotserver/tomcat/webapps/ROOT/dotsecure/logs/test/*.xml tests

# Run Integration tests
cd core/dotCMS
./gradlew integrationTest -PdatabaseType=$DB_TYPE  || true
cd ../..
cp core/dotCMS/build/test-results/integrationTest/*.xml tests


# Create output zip file
zip ../$OUTPUT_FILE tests/*.*
