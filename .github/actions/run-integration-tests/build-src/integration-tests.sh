#!/usr/bin/env bash

###############################
# Script: integrations-tests.sh
# Collection of functions that support running integration tests

export CUSTOM_FOLDER=/srv/custom
license_folder='dotsecure/license'
export LICENSE_FOLDER=${CUSTOM_FOLDER}/${license_folder}
it_folders=(
  'assets'
  'dotsecure'
  'esdata'
  'felix'
  'output/logs'
  'output/reports/html'
  "${license_folder}"
)


# Crates/Copies necessary folders/files  to run integration tests
function prepareTests {
  mkdir -p ${CUSTOM_FOLDER}
  for folder in "${it_folders[@]}"
  do
    executeCmd "mkdir -p ${CUSTOM_FOLDER}${folder}"
  done

  executeCmd "cp ${INPUT_PROJECT_ROOT}/cicd/docker/*.yml ."
  executeCmd "mkdir -p setup/db"
  executeCmd "cp -R ${INPUT_PROJECT_ROOT}/cicd/setup/${INPUT_DB_TYPE} ./setup/db"
  executeCmd "cp ${INPUT_PROJECT_ROOT}/cicd/resources/* ${INPUT_PROJECT_ROOT}/dotCMS/src/test/resources"
}

# Resolves integration tests parameters
function resolveItParams {
  local test_suite_command="-Dtest.single=com.dotcms.MainSuite"
  local it_cmd_params=${test_suite_command}

  if [[ "${BUILD_ENV}" == 'gradle' ]]; then
    if [[ -n "${INPUT_EXTRA_PARAMS}" ]]; then
      it_cmd_params=${INPUT_EXTRA_PARAMS}
      [[ ${INPUT_EXTRA_PARAMS} =~ '--tests' ]] \
        && it_cmd_params="${it_cmd_params} ${test_suite_command}"
    fi
  elif [[ "${BUILD_ENV}" == 'maven' ]]; then
    # TODO: add maven support
    it_cmd_params="${it_cmd_params}"
  fi

  echo ${it_cmd_params}
}

# Resolves actual integration tests command
function resolveItCmd {
  local it_cmd="${BUILD_TOOL}"
  if [[ "${BUILD_ENV}" == 'gradle' ]]; then
    it_cmd="${it_cmd} integrationTest"
  elif [[ "${BUILD_ENV}" == 'maven' ]]; then
    # TODO: add maven support
    it_cmd="${it_cmd}"
  fi
  local it_cmd_params=$(resolveItParams)
  echo "${it_cmd} ${it_cmd_params}"
}

# Overrides config files with necessary values fot ITs to run
function overrideProps {
  local assets_folder=/custom/assets
  local dot_secure_folder=/custom/dotsecure
  local es_data_folder=/custom/esdata
  local logs_folder=/custom/output/logs
  local felix_folder=/custom/felix
  local dotcms_folder="${INPUT_PROJECT_ROOT}/dotCMS"
  local resources_folder="${dotcms_folder}/src/integration-test/resources"

  sed -i "s,^# integrationTestFelixFolder=.*$,integrationTestFelixFolder=${felix_folder},g" ${dotcms_folder}/gradle.properties
  sed -i "s,^#DYNAMIC_CONTENT_PATH=.*$,DYNAMIC_CONTENT_PATH=${dot_secure_folder},g" ${dotcms_folder}/src/main/resources/dotmarketing-config.properties
  sed -i "s,^es.path.home=.*$,es.path.home=${dotcms_folder}/src/main/webapp/WEB-INF/elasticsearch,g" ${resources_folder}/it-dotcms-config-cluster.properties
  sed -i "s,^ES_HOSTNAME=.*$,ES_HOSTNAME=elasticsearch,g" ${resources_folder}/it-dotcms-config-cluster.properties
  sed -i "s,^DYNAMIC_CONTENT_PATH=.*$,DYNAMIC_CONTENT_PATH=${dot_secure_folder},g" ${resources_folder}/it-dotmarketing-config.properties
  sed -i "s,^TAIL_LOG_LOG_FOLDER=.*$,TAIL_LOG_LOG_FOLDER=${dot_secure_folder}/logs/,g" ${resources_folder}/it-dotmarketing-config.properties
  sed -i "s,^ASSET_REAL_PATH =.*$,ASSET_REAL_PATH=${assets_folder},g" ${resources_folder}/it-dotmarketing-config.properties
  sed -i "s,^#TOOLBOX_MANAGER_PATH=.*$,TOOLBOX_MANAGER_PATH=${dotcms_folder}/src/main/webapp/WEB-INF/toolbox.xml,g" ${resources_folder}/it-dotmarketing-config.properties
  sed -i "s,^VELOCITY_ROOT =.*$,VELOCITY_ROOT=${dotcms_folder}/src/main/webapp/WEB-INF/velocity,g" ${resources_folder}/it-dotmarketing-config.properties
  sed -i "s,^GEOIP2_CITY_DATABASE_PATH_OVERRIDE=.*$,GEOIP2_CITY_DATABASE_PATH_OVERRIDE=${dotcms_folder}/src/main/webapp/WEB-INF/geoip2/GeoLite2-City.mmdb,g" ${resources_folder}/it-dotmarketing-config.properties
  sed -i "s,^felix.base.dir=.*$,felix.base.dir=${felix_folder},g" ${resources_folder}/it-dotmarketing-config.properties

  echo "felix.felix.fileinstall.dir=${felix_folder}/load" >> ${resources_folder}/it-dotmarketing-config.properties
  echo "felix.felix.undeployed.dir=${felix_folder}/undeploy" >> ${resources_folder}/it-dotmarketing-config.properties
  echo "dotcms.concurrent.locks.disable=false" >> ${resources_folder}/it-dotmarketing-config.properties
  echo "
  cluster.name: dotCMSContentIndex_docker
  path.data: ${es_data_folder}
  path.repo: ${es_data_folder}/essnapshot/snapshots
  path.logs: ${logs_folder}
  # http.port: 9200
  # transport.tcp.port: 9309

  http.enabled: false
  http.cors.enabled: false

  # http.host: localhost

  cluster.routing.allocation.disk.threshold_enabled: false
  " >> ${dotcms_folder}/src/main/webapp/WEB-INF/elasticsearch/config/elasticsearch-override.yml
}

# Run Integration Tests
function runIntegrationTests {
  cd ${INPUT_PROJECT_ROOT}/dotCMS
  echo "
  =========================
  Running Integration Tests
  =========================
  INPUT_PROJECT_ROOT: ${INPUT_PROJECT_ROOT}
  BUILD_ENV: ${BUILD_ENV}
  DB_TYPE: ${DB_TYPE}
  EXTRA_PARAMS: ${EXTRA_PARAMS}
  "
  it_cmd=$(resolveItCmd)
  executeCmd "${it_cmd}"

  setOutputs ${cmd_result} "${INPUT_PROJECT_ROOT}/dotCMS/build/reports/tests/integrationTest"

  return ${cmd_result}
}