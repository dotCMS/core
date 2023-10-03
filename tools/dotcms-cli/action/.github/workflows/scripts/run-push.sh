#!/bin/sh

  ## https://repo.dotcms.com/artifactory/libs-snapshot-local/com/dotcms/dotcms-cli/1.0.0-SNAPSHOT/dotcli-1.0.0-SNAPSHOT.jar
  CLI_RELEASE_DOWNLOAD_BASE_URL="https://repo.dotcms.com/artifactory/libs-snapshot-local/com/dotcms/dotcms-cli/"
  RUN_JAVA_VERSION=1.3.8
  RUN_DOT_CLI_VERSION='1.0.0-SNAPSHOT'
  CLI_RELEASE_DOWNLOAD_URL="${CLI_RELEASE_DOWNLOAD_BASE_URL}${RUN_DOT_CLI_VERSION}/dotcli-${RUN_DOT_CLI_VERSION}.jar"
  DOT_CLI_JAR="dot-cli.jar"
  DOT_CLI_HOME="/tmp/dot-cli/"
  DOT_SERVICE_YML="dot-service.yml"

SERVICES_FILE_CONTENT='
- name: "default"
  active: true
'

_make_home(){

  if [ ! -d "$DOT_CLI_HOME" ]; then
    mkdir $DOT_CLI_HOME
  fi
  #echo $DOT_CLI_HOME
}

_get_CLI(){
  # now lets get curl so we can download the CLI and the run-java.sh script

  #echo "downloading dot CLI from ${CLI_RELEASE_DOWNLOAD_URL}"
  curl ${CLI_RELEASE_DOWNLOAD_URL} -L -o ${DOT_CLI_HOME}${DOT_CLI_JAR}
  chmod 777 "${DOT_CLI_HOME}${DOT_CLI_JAR}"

  #Check the size of the file
  file="${DOT_CLI_HOME}${DOT_CLI_JAR}" && \
      actual_size=$(wc -c <"$file");

  if [ "$actual_size" -lt 1000000 ]; then
    echo "The file is too small to be the CLI, please check the version and try again"
    exit 1
  fi
}

_install_CLI(){
  workspace_path=$1
  cp "$workspace_path"/dotcms-cli-1.0.0-SNAPSHOT-runner.jar ${DOT_CLI_HOME}${DOT_CLI_JAR}
}

_get_run_java_script(){
    #echo "downloading run-java.sh"
    curl https://repo1.maven.org/maven2/io/fabric8/run-java-sh/${RUN_JAVA_VERSION}/run-java-sh-${RUN_JAVA_VERSION}-sh.sh -o "${DOT_CLI_HOME}"run-java.sh
    chmod 777 ${DOT_CLI_HOME}run-java.sh
}

_setup_CLI(){
    API_URL=$1
    #Lets create the services file dot-service.yml
    #the services yml is used to store the server configurations or profiles if you Will
    DOT_SERVICES_HOME=$HOME/.dotcms/
    SERVICE_FILE=$DOT_SERVICES_HOME$DOT_SERVICE_YML
   # All we need is a file with an active profile that matches the server we want to connect to in this case we are using default

    if [ ! -d "$DOT_SERVICES_HOME" ]; then
      mkdir "$DOT_SERVICES_HOME"
      ## echo creating ::  "$SERVICE_FILE";
      echo "$SERVICES_FILE_CONTENT" >> "$SERVICE_FILE";
      #echo created file :: "$SERVICE_FILE"
      #cat "$SERVICE_FILE";
    fi

    #Tell the CLI to use the demo server through the profile "default"
    #The suffix value used to create the environment value must match the name on dot-service.yml file in this case we are using default
    #dotcms.client.servers.default=https://demo.dotcms.com/api

    export DOTCMS_CLIENT_SERVERS_DEFAULT=$API_URL
    export QUARKUS_LOG_FILE_PATH=$DOT_CLI_HOME"dotcms-cli.log"

}

print_log(){
  echo "Quarkus log file contents:"
  cat "$QUARKUS_LOG_FILE_PATH"
}

_run_cli_push(){
      workspace_path=$1
      token=$2

      #These environment vars are expected by the start-up script
      export JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
      # This is a relative path to the run-java.sh file, both the jar and script are expected to live in the same folder
      export JAVA_APP_JAR="$DOT_CLI_JAR"
      # This is the name of the process that will be used to identify the process in the container
      export JAVA_APP_NAME="dotcms-cli"
      # Log file
      export QUARKUS_LOG_FILE_PATH="$DOT_CLI_HOME"dotcms-cli.log
      bash /tmp/dot-cli/run-java.sh "push" "$workspace_path" "--removeAssets" "--removeFolders" "--token" "$token" "--errors"
      export exit_code=$?
      echo $exit_code
}

run_cli_push(){
    workspace_path=$1
    dotApiURL=$2
    token=$3
    _make_home
    #_get_CLI
    _install_CLI "$workspace_path"
    _get_run_java_script
    _setup_CLI "$dotApiURL"
    return_code=$(_run_cli_push "$workspace_path" "$token")
    echo "$return_code"
}