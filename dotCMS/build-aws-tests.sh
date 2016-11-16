#!/usr/bin/env bash


# Enable echoing commands
trap 'echo "[$USER@$(hostname) $PWD]\$ $BASH_COMMAND"' DEBUG


# Create working directory
mkdir repo
cd repo


# Check out branch under working directory
git clone -b $BRANCH https://github.com/dotCMS/core.git


# Build tests and distro
cd core/dotCMS
./gradlew --stop
./gradlew clean --no-daemon --refresh-dependencies
./gradlew copyTestRuntimeLibs individualTestJar integrationTestJar functionalTestJar --no-daemon
./gradlew createDist --no-daemon 
cd ../..


# Uncompress distro and tomcat under working directory
tar zxf core/dist-output/dotcms_*.tgz
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

sed -i "s,^db.driver=.*$,db.driver=$DB_DRIVER,g" core/dotCMS/src/integration-test/resources/db-config.properties
sed -i "s,^db.base.url=.*$,db.base.url=$DB_URL,g" core/dotCMS/src/integration-test/resources/db-config.properties
sed -i "s,^db.username=.*$,db.username=$DB_USERNAME,g" core/dotCMS/src/integration-test/resources/db-config.properties
sed -i "s,^db.password=.*$,db.password=$DB_PASSWORD,g" core/dotCMS/src/integration-test/resources/db-config.properties


# Create output directory
mkdir tests


# Run End-2-End tests
ant -f build-tests.xml test-dotcms
cp dotserver/tomcat/webapps/ROOT/dotsecure/logs/test/*.xml tests

# Run Integration tests
cd core/dotCMS
./gradlew integrationTest --no-daemon || true
cd ../..
cp core/dotCMS/build/test-results/integrationTest/*.xml tests


# Create output zip file
zip ../$OUTPUT_FILE tests/*.*
