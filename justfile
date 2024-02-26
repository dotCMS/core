set positional-arguments := true
home_dir := env_var('HOME')
# Introduction and Setup
# If homebrew is not installed, run the following command:
# /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)";
# If git is not installed run:
# brew install git
# clone this repository with
# git clone https://github.com/dotcms/core.git
# cd core
# just a tool for running commands defined in this file and provides useful aliases for common tasks
# you can also just use the commands listed below directly in your terminal
# To install "just", use "brew install just". Optionally, install the IntelliJ Plugin from https://plugins.jetbrains.com/plugin/18658-just. A cheatsheet is available at https://cheatography.com/linux-china/cheat-sheets/justfile/.
# You can then check the remaining dependencies with "just install-all-mac-deps"
# For all tests to run you need a license key. You can drop the license.dat file into ~/.dotcms/license/license.dat
# Test everything builds and reset your environment with "just build"

###########################################################
# Core Commands
###########################################################

# Lists all available commands in this justfile
default:
    @just --list --unsorted --justfile {{ justfile() }}


# Builds the project without running tests, useful for quick iterations
build:
    ./mvnw -DskipTests clean install

# Builds the project without running tests, skip using docker or creating image
build-no-docker:
    ./mvnw -DskipTests clean install -Dskip.docker=true

# Builds the project and runs the default test suite
build-test:
    ./mvnw clean install

# Performs a quick build, installing all modules to the local repository without running tests
build-quick:
    ./mvnw -DskipTests install

# Builds the project for production, skipping tests
build-prod:
    ./mvnw -DskipTests clean install -Pprod

# Runs a comprehensive test suite including core integration and postman tests, suitable for final validation
build-test-full:
    ./mvnw clean install -Dcoreit.test.skip=false -Dpostman.test.skip=false

# Builds a specified module without its dependencies, defaulting to the core server (dotcms-core)
build-select-module module="dotcms-core":
    ./mvnw install -pl :{{ module }} -DskipTests=true

# Builds a specified module along with its required dependencies
build-select-module-deps module=":dotcms-core":
    ./mvnw install -pl {{ module }} --am -DskipTests=true

# Development Commands

# Starts the dotCMS application in a Docker container on a dynamic port, running in the foreground
dev-run:
    ./mvnw -pl :dotcms-core -Pdocker-run

# Maps paths in the docker container to local paths, useful for development
dev-run-map-dev-paths:
    ./mvnw -pl :dotcms-core -Pdocker-run -Pmap-dev-paths

# Starts the dotCMS application in debug mode with suspension, useful for troubleshooting
dev-run-debug-suspend:
    ./mvnw -pl :dotcms-core -Pdocker-run,debug-suspend

# Starts the dotCMS Docker container in the background
dev-start:
    ./mvnw -pl :dotcms-core -Pdocker-start

# Stops the development Docker container
dev-stop:
    ./mvnw -pl :dotcms-core -Pdocker-stop

# Cleans up Docker volumes associated with the development environment
dev-clean-volumes:
    ./mvnw -pl :dotcms-core -Pdocker-clean-volumes

# Starts the dotCMS application in a Tomcat container on port 8080, running in the foreground
dev-tomcat-run port="8087":
    ./mvnw -pl :dotcms-core -Ptomcat-run -Pdebug -Dservlet.port={{ port }}

dev-tomcat-stop:
    ./mvnw -pl :dotcms-core -Ptomcat-stop -Dcontext.name=local-tomcat

# Testing Commands

# Executes a specified set of Postman tests
test-postman collections='':
    ./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false -Pdebug -Dpostman.collections={{ collections }}

# Stops Postman-related Docker containers
postman-stop:
    ./mvnw -pl :dotcms-postman -Pdocker-stop

# Runs all integration tests
test-integration:
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false

# Suspends execution for debugging integration tests
test-integration-debug-suspend:
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Pdebug-suspend

# Prepares the environment for running integration tests in an IDE
test-integration-ide:
    ./mvnw -pl :dotcms-integration -Pdocker-start

# Stops integration test services
test-integration-stop:
    ./mvnw -pl :dotcms-integration -Pdocker-stop
# Docker Commands

# Runs a published dotCMS Docker image on a dynamic port
docker-ext-run tag='latest':
    ./mvnw -pl :dotcms-core -Pdocker-start -Dcontext.name=ext-{{ tag }} -Ddotcms.image.name=dotcms/dotcms:{{ tag }}

# Runs a Docker image from a specific build for testing
docker-test-ext-run tag='master':
    ./mvnw -pl :dotcms-core -Pdocker-start -Dcontext.name=test-ext-{{ tag }} -Ddotcms.image.name=ghcr.io/dotcms/dotcms_test:{{ tag }}

# Stops a running Docker container based on the specified tag
docker-ext-stop tag='latest':
    ./mvnw -pl :dotcms-core -Pdocker-stop -Dcontext.name=ext-{{ tag }}

###########################################################
# Useful Maven Helper Commands
###########################################################

# Generates a dependency tree for the compile scope and saves it to a file
maven-dependencies:
    ./mvnw dependency:tree -Dscope=compile > dependencies.txt

# Displays updates for project dependencies in a text file
maven-updates:
    ./mvnw versions:display-dependency-updates > updates.txt

# Checks for updates to project dependencies and prints to console
maven-check-updates:
    ./mvnw versions:display-dependency-updates

# Checks for updates to Maven plugins used in the project
maven-check-plugin-updates:
    ./mvnw versions:display-plugin-updates

# Checks for updates to properties defined in the pom.xml that control dependency versions
maven-property-updates:
    ./mvnw versions:display-property-updates


###########################################################
# Dependency Commands
###########################################################

# Installs all dependencies for the current project
install-all-mac-deps: install-jdk-mac check-docker-mac check-git-mac

# Installs SDKMAN for managing Java JDK versions
install-sdkman-mac:
    @if [ -d "${HOME}/.sdkman/bin" ]; then \
        echo "SDKMAN is already installed."; \
    else \
        echo "SDKMAN is not installed, installing now..."; \
        curl -s "https://get.sdkman.io" | bash; \
        source "${HOME}/.sdkman/bin/sdkman-init.sh"; \
    fi

# Installs the latest version of Java JDK using SDKMAN
install-jdk-mac: install-sdkman-mac
    #!/usr/bin/env bash
    set -eo pipefail
    source ~/.sdkman/bin/sdkman-init.sh
    sdk env install java

# Checks if Docker is installed and running
check-docker-mac:
    @if ! command -v docker >/dev/null; then \
        echo "Docker is not installed."; \
        echo "Install docker with : brew install docker --cask"; \
        echo " or download from : https://docs.docker.com/get-docker/"; \
        exit 1; \
    else \
        echo "Docker is installed."; \
        if ! docker info >/dev/null 2>&1; then \
            echo "Docker is not running ..."; \
            exit 1; \
        else \
            echo "Docker is running."; \
        fi; \
    fi

check-git-mac:
    @if ! command -v git >/dev/null; then \
        echo "Git is not installed. Installing Git..."; \
        brew install git; \
    else \
        git --version; \
        echo "Git is already installed."; \
    fi
