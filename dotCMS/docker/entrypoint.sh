#!/bin/sh

## entrypoint.sh
cd ${DOTCMS_HOME}

cp plugins-dist/common.xml plugins-dist/plugins.xml plugins/

./bin/deploy-plugins.sh && ./bin/startup.sh run 