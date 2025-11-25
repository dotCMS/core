#!/bin/sh

  DOT_CLI_JAR="dot-cli.jar"
  DOT_CLI_HOME="/tmp/dot-cli/"
  DOT_SERVICE_YML="dot-service.yml"
  RUN_JAVA_VERSION=1.3.8

SERVICES_FILE_CONTENT="
- name: \"default\"
  active: true
  url: \"$DOT_API_URL\"
"

_make_home(){
  if [ ! -d "$DOT_CLI_HOME" ]; then
    mkdir $DOT_CLI_HOME
  fi
}

_get_CLI(){

  cli_release_download_url=$1
  force_download=$2

  cliJar=${DOT_CLI_HOME}${DOT_CLI_JAR}
  if [ -f $cliJar ] && ! [ "$force_download" = true  ] ; then
    echo "dot-CLI already exists, skipping download"
    return
  fi

  curl "$cli_release_download_url" -L -o ${DOT_CLI_HOME}${DOT_CLI_JAR}
  chmod 777 "${DOT_CLI_HOME}${DOT_CLI_JAR}"

  #Check the size of the file
  file="${DOT_CLI_HOME}${DOT_CLI_JAR}" && \
      actual_size=$(wc -c <"$file");

  if [ "$actual_size" -lt 1000000 ]; then
    echo "The file is too small to be the CLI, please check the version and try again"
    exit 1
  fi
}

_get_run_java_script(){
   force_download=$1
   runJava=${DOT_CLI_HOME}run-java.sh
   if [ -f "$runJava" ] && ! [ "$force_download" = true  ] ; then
      echo "run-java.sh already exists, skipping download"
      return
   fi
    curl https://repo1.maven.org/maven2/io/fabric8/run-java-sh/"${RUN_JAVA_VERSION}"/run-java-sh-"${RUN_JAVA_VERSION}"-sh.sh -o "${DOT_CLI_HOME}"run-java.sh
    chmod 777 ${DOT_CLI_HOME}run-java.sh
}

_setup_CLI(){
    #Lets create the services file dot-service.yml
    #the services yml is used to store the server configurations or profiles if you Will
    DOT_SERVICES_HOME=$HOME/.dotcms/
    SERVICE_FILE=$DOT_SERVICES_HOME$DOT_SERVICE_YML
   # All we need is a file with an active profile that matches the server we want to connect to in this case we are using default
   # If the directory does not exist we create it
    if [ ! -d "$DOT_SERVICES_HOME" ]; then
      mkdir "$DOT_SERVICES_HOME"
    else
       # If the directory exists we remove it as we could be updating the server url
       rm -rf "$DOT_SERVICES_HOME"
       mkdir "$DOT_SERVICES_HOME"
    fi
    # Now generate the file
    echo "$SERVICES_FILE_CONTENT" >> "$SERVICE_FILE";

    export QUARKUS_LOG_FILE_PATH=$DOT_CLI_HOME"dotcms-cli.log"
}

print_log(){
  echo "Quarkus log file contents:"
  cat "$QUARKUS_LOG_FILE_PATH"
}

_run_cli_push(){
      workspace_path=$1
      token=$2
      push_opts=$3

      #These environment vars are expected by the start-up script
      export JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
      # This is a relative path to the run-java.sh file, both the jar and script are expected to live in the same folder
      export JAVA_APP_JAR="$DOT_CLI_JAR"
      # This is the name of the process that will be used to identify the process in the container
      export JAVA_APP_NAME="dotcms-cli"
      # Log file
      export QUARKUS_LOG_FILE_PATH="$DOT_CLI_HOME"dotcms-cli.log
      cmd="bash /tmp/dot-cli/run-java.sh push $workspace_path $push_opts --token=$token"
      eval "$cmd"
      export exit_code=$?
      echo $exit_code
}

install_cli(){
    cli_release_download_url=$1
    force_download=$2

    _make_home
    _get_CLI "$cli_release_download_url" "$force_download"
    _get_run_java_script "$force_download"
    _setup_CLI
}

run_cli_push(){
    workspace_path=$1
    token=$2
    push_opts=$3

    echo "PUSH OPTS:"
    echo "$push_opts"

    return_code=$(_run_cli_push "$workspace_path" "$token" "$push_opts")
    echo "$return_code"
}
