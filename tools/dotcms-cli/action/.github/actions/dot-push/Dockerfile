FROM openjdk:11
LABEL authors="fabrizzioaraya"

ARG CLI_RELEASE_DOWNLOAD_BASE_URL="https://repo.dotcms.com/artifactory/libs-snapshot-local/com/dotcms/dotcms-cli/"
ARG RUN_JAVA_VERSION=1.3.8
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ARG RUN_DOT_CLI_VERSION='1.0.0-SNAPSHOT'
ARG CLI_RELEASE_DOWNLOAD_URL="${CLI_RELEASE_DOWNLOAD_BASE_URL}${RUN_DOT_CLI_VERSION}/dotcli-${RUN_DOT_CLI_VERSION}.jar"
ENV DOT_CLI_JAR="dot-cli.jar"
ENV DEMO_API_URL="https://demo.dotcms.com/api"
ENV DOT_SERVICES_YML="dot-service.yml"
ENV ENTRYPOINT="entrypoint.sh"
ENV DOT_CLI_HOME=/dot-cli/

#the services yml is used to store the server configurations or profiles if you Will
ENV USER_HOME="/root/"
ENV DOT_SERVICES_HOME=${USER_HOME}".dotcms/"
#now move the file into the .dotcms folder located uder the user home folder
COPY ${DOT_SERVICES_YML} ${DOT_SERVICES_HOME}

# Lets crate the dot-cli home folder and give it the right permissions
RUN mkdir -p ${DOT_CLI_HOME}
RUN chmod 777 ${DOT_CLI_HOME}
#Copy entry point
COPY ${ENTRYPOINT} ${DOT_CLI_HOME}

#Tell the CLI to use the demo server
#The sufix value used to create the environment value must match the name on dot-service.yml file in this case we are using default
#dotcms.client.servers.default=https://demo.dotcms.com/api
ENV DOTCMS_CLIENT_SERVERS_DEFAULT=$DOT_API_URL
ENV DOTCMS_CLIENT_SERVERS_DEFAULT=${DOTCMS_CLIENT_SERVERS_DEFAULT:-$DEMO_API_URL}

#Set the var that tells the CLI where to store the log file
ENV QUARKUS_LOG_FILE_PATH=${DOT_CLI_HOME}"dotcms-cli.log"

# now lets get curl so we can download the CLI and the run-java.sh script
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean;

RUN echo "downloading dot CLI from ${CLI_RELEASE_DOWNLOAD_URL}"

#Download the CLI from our relase repo and story as dot-cli.jar
RUN curl ${CLI_RELEASE_DOWNLOAD_URL} -L -o ${DOT_CLI_HOME}${DOT_CLI_JAR}
RUN chmod 777 "${DOT_CLI_HOME}${DOT_CLI_JAR}"

#Check the size of the file
RUN file="${DOT_CLI_HOME}${DOT_CLI_JAR}" && \
    actual_size=$(wc -c <"$file");

RUN if (( $actual_size > 0 )); then \
       echo "dotcms-cli file size is $actual_size "; \
    else \
       echo "dot-CLI size is 0 bytes - Terminating program"; \
       exit 1; \
    fi

RUN echo "downloading run-java.sh"
RUN curl https://repo1.maven.org/maven2/io/fabric8/run-java-sh/${RUN_JAVA_VERSION}/run-java-sh-${RUN_JAVA_VERSION}-sh.sh -o "${DOT_CLI_HOME}"run-java.sh
RUN chmod 777 ${DOT_CLI_HOME}run-java.sh

#These environment vars are expected by the start-up script
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
# This is a relative path to the run-java.sh file, both the jar and script are expected to live in the same folder
ENV JAVA_APP_JAR="${DOT_CLI_JAR}"
# This is the name of the process that will be used to identify the process in the container
ENV JAVA_APP_NAME="dotcms-cli"

# The following are here to enable debug mode
#ENV JAVA_DEBUG="y"
#ENV JAVA_DEBUG_SUSPEND="y"
#ENV JAVA_DEBUG_PORT="5005"
#EXPOSE 5005

# Code file to execute when the docker container starts up (`entrypoint.sh`)

RUN echo "RUN_JAVA_VERSION IS ${RUN_JAVA_VERSION}"
RUN echo "DOT_CLI VERSION IS: ${RUN_DOT_CLI_VERSION}"
RUN echo "DOT_CLI_HOME IS: ${DOT_CLI_HOME}"
RUN echo "DOTCMS_CLIENT_SERVERS_DEFAULT IS: ${DOTCMS_CLIENT_SERVERS_DEFAULT}"
RUN echo "LOG FILE IS: ${QUARKUS_LOG_FILE_PATH}"

ENTRYPOINT ["/dot-cli/entrypoint.sh"]