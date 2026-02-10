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
    ./mvnw -DskipTests clean install -Ddocker.skip

# Builds the project and runs the default test suite
build-test:
    ./mvnw clean install

# Performs a quick build, installing all modules to the local repository without running tests
build-quick:
    ./mvnw -DskipTests install

build-quicker:
    ./mvnw -pl :dotcms-core -DskipTests install

# Builds the project for production, skipping tests
build-prod:
    ./mvnw -DskipTests clean install -Pprod

# Builds core-web module
build-core-web:
    ./mvnw clean install -pl :dotcms-core-web -am -DskipTests

# Builds core-web module with Nx cache reset
build-core-web-reset-nx:
    ./mvnw clean install -pl :dotcms-core-web -am -DskipTests -Dnx.reset

# Builds core-web module with Nx cache reset
build-test-core-web:
    ./mvnw clean install -pl :dotcms-core-web -am

# Runs a comprehensive test suite including core integration and postman tests, suitable for final validation
build-test-full:
    ./mvnw clean install -Dcoreit.test.skip=false -Dpostman.test.skip=false -Dkarate.test.skip=false

# Builds a specified module without its dependencies, defaulting to the core server (dotcms-core)
build-select-module module="dotcms-core":
    ./mvnw install -pl :{{ module }} -DskipTests=true

# Builds a specified module along with its required dependencies
build-select-module-deps module=":dotcms-core":
    ./mvnw install -pl {{ module }} --am -DskipTests=true

# Development Commands
dev-run:
    ./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.glowroot.enabled=true

# Starts the dotCMS application in a Docker container on a dynamic port, running in the foreground
dev-run-debug:
    ./mvnw -pl :dotcms-core -Pdocker-start,debug

# Maps paths in the docker container to local paths, useful for development
dev-run-map-dev-paths:
    ./mvnw -pl :dotcms-core -Pdocker-start -Pmap-dev-paths

# Starts the dotCMS application in debug mode with suspension, useful for troubleshooting
dev-run-debug-suspend port="8082":
    ./mvnw -pl :dotcms-core -Pdocker-start,debug-suspend -Dtomcat.port={{ port }}

# Starts the dotCMS Docker container in the background
dev-start-on-port port="8082":
    ./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port={{ port }}

# Stops the development Docker container
dev-stop:
    ./mvnw -pl :dotcms-core -Pdocker-stop

# Cleans up Docker volumes associated with the development environment
dev-clean-volumes:
    ./mvnw -pl :dotcms-core -Pdocker-clean-volumes

# Starts the dotCMS application in a Tomcat container on port 8087, running in the foreground
dev-tomcat-run port="8087":
    ./mvnw -pl :dotcms-core -Ptomcat-run -Pdebug -Dservlet.port={{ port }}

dev-tomcat-stop:
    ./mvnw -pl :dotcms-core -Ptomcat-stop -Dcontext.name=local-tomcat

# Starts dotCMS with JMX monitoring enabled for connecting from localhost
dev-run-jmx:
    ./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true

# Starts dotCMS with JMX monitoring on custom ports
dev-run-jmx-ports jmx_port="9999" rmi_port="9998":
    ./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true -Djmx.port={{ jmx_port }} -Djmx.rmi.port={{ rmi_port }}

# Starts dotCMS with both JMX monitoring and debug enabled
dev-run-jmx-debug:
    ./mvnw -pl :dotcms-core -Pdocker-start,jmx-debug -Djmx.debug.enable=true

# Starts dotCMS with JMX, debug, and Glowroot profiler enabled
dev-run-jmx-debug-glowroot:
    ./mvnw -pl :dotcms-core -Pdocker-start,jmx-debug,glowroot -Djmx.debug.enable=true -Ddocker.glowroot.enabled=true

# Testing Commands

# Executes a specified set of Postman tests
test-postman collections='ai':
    ./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false -Pdebug -Dpostman.collections={{ collections }}

# Stops Postman-related Docker containers
postman-stop:
    ./mvnw -pl :dotcms-postman -Pdocker-stop -Dpostman.test.skip=false

test-karate collections='KarateCITests#defaults':
    ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Pdebug -Dit.test={{ collections }}

# Runs all integration tests
test-integration:
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false

# Runs only the open-search integration tests
test-integration-open-search:
   ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true

# Suspends execution for debugging integration tests
test-integration-debug-suspend:
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Pdebug-suspend

# Just rebuild core and its deps without docker, e.g. pickup changes for it tests
build-core-with-deps:
    ./mvnw install -pl :dotcms-core --am -DskipTests -Ddocker.skip=true

# Just rebuild core without docker, e.g. pickup changes for it tests
build-core-only:
    ./mvnw install -pl :dotcms-core  -DskipTests -Ddocker.skip=true

# Prepares the environment for running integration tests in an IDE
test-integration-ide:
    ./mvnw -pl :dotcms-integration pre-integration-test -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true -Dtomcat.port=8080

# Stops integration test services
test-integration-stop:
    ./mvnw -pl :dotcms-integration -Pdocker-stop -Dcoreit.test.skip=false

test-postman-ide:
    ./mvnw -pl :dotcms-test-postman pre-integration-test -Dpostman.test.skip=false -Dtomcat.port=8080

test-karate-ide:
    ./mvnw -pl :dotcms-test-karate pre-integration-test -Dkarate.test.skip=false -Dtomcat.port=8080

# Executes Java E2E tests
test-e2e-java:
    ./mvnw -pl :dotcms-e2e-java verify -De2e.test.skip=false

# Suspends execution for debugging Java E2E tests
test-e2e-java-debug-suspend:
    ./mvnw -pl :dotcms-e2e-java verify -De2e.test.skip=false -Pdebug-suspend-e2e-tests

# Executes Node E2E tests
test-e2e-node:
    ./mvnw -pl :dotcms-e2e-node verify -De2e.test.skip=false

# The `e2e.test.specific` param can be a single test or a list of directories, please refer to: https://playwright.dev/docs/running-tests#run-specific-tests
test-e2e-node-specific test="login.spec.ts":
    ./mvnw -pl :dotcms-e2e-node verify -De2e.test.skip=false -De2e.test.specific="{{ test }}"

# Starts a de debug session using Playwright's UI mode, please refer to: https://playwright.dev/docs/test-ui-mode
# The `e2e.test.specific` param can be a single test or a list of directories, please refer to: https://playwright.dev/docs/running-tests#run-specific-tests
test-e2e-node-debug-ui test="login.spec.ts":
    ./mvnw -pl :dotcms-e2e-node verify -De2e.test.skip=false -De2e.test.debug="--ui" -De2e.test.specific="{{ test }}"

# Starts a de debug session using Playwright's debug inspector, please refer to: https://playwright.dev/docs/running-tests#debug-tests-with-the-playwright-inspector
# The `e2e.test.specific` param can be a single test or a list of directories, please refer to: https://playwright.dev/docs/running-tests#run-specific-tests
test-e2e-node-debug test="login.spec.ts":
    ./mvnw -pl :dotcms-e2e-node verify -De2e.test.skip=false -De2e.test.debug="--debug" -De2e.test.specific="{{ test }}"

# Stops E2E test services
test-e2e-stop:
    ./mvnw -pl :dotcms-e2e-java,:dotcms-e2e-node -Pdocker-stop -De2e.test.skip=false

# Docker Commands
# Runs a published dotCMS Docker image on a dynamic port
docker-ext-run tag='latest':
    ./mvnw -pl :dotcms-core -Pdocker-start -Dcontext.name=ext-{{ tag }} -Ddotcms.image.name=dotcms/dotcms:{{ tag }}

# Runs a Docker image from a specific build for testing
docker-test-ext-run tag='main':
    ./mvnw -pl :dotcms-core -Pdocker-start -Dcontext.name=test-ext-{{ tag }} -Ddotcms.image.name=ghcr.io/dotcms/dotcms_test:{{ tag }}

# Stops a running Docker container based on the specified tag
docker-ext-stop tag='latest':
    ./mvnw -pl :dotcms-core -Pdocker-stop -Dcontext.name=ext-{{ tag }}

# Generate a cli uber-jar
cli-build-uber-jar:
    ./mvnw -pl :dotcms-cli package -DskipTests=true

cli-build-native: check-native-deps
    #!/usr/bin/env bash
    set -eo pipefail # Exit on error
    source ${HOME}/.sdkman/bin/sdkman-init.sh  # Load SDKMAN normally in you .bashrc or .zshrc
    # The above is not required when running from your shell if you have SDKMAN in your .bashrc or .zshrc
    sdk env install # ensure right version of java is downloaded and used for the branch
    sdk install java 21.0.2-graalce # Does nothing if already installed
    export GRAALVM_HOME=$(sdk home java 21.0.2-graalce) # Could be added to your .bashrc or .zshrc
    ./mvnw -pl :dotcms-cli -DskipTests -Pnative package


cli-build-test-native: check-native-deps
    #!/usr/bin/env bash
    set -eo pipefail
    source ${HOME}/.sdkman/bin/sdkman-init.sh
    # The above is not required when running from your shell if you have SDKMAN in your .bashrc or .zshrc
    sdk env install # ensure right version of java is downloaded and used for the branch
    sdk install java 21.0.2-graalce # Does nothing if already installed
    export GRAALVM_HOME=$(sdk home java 21.0.2-graalce) # Env var Could be added to your .bashrc or .zshrc
    ./mvnw -pl :dotcms-cli -Pnative verify

run-built-cli *ARGS:
    java -jar tools/dotcms-cli/cli/target/dotcms-cli-1.0.0-SNAPSHOT-runner.jar {{ARGS}}


run-java-cli-native *ARGS:
    tools/dotcms-cli/cli/target/dotcms-cli-1.0.0-SNAPSHOT-runner {{ARGS}}


run-jmeter-tests:
    ./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter

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

# Check if xcode is installed if on a mac.  Only required for native builds
check-native-deps:
    #!/usr/bin/env bash
    set -eo pipefail
    # Determine the operating system
    OS=$(uname)

    # Install dependencies based on the package manager
    if [ "$OS" = "Linux" ]; then
        if command -v apt-get >/dev/null; then
            sudo apt-get update
            sudo apt-get install -y build-essential libz-dev zlib1g-dev
        elif command -v dnf >/dev/null; then
            sudo dnf install -y gcc glibc-devel zlib-devel libstdc++-static
        else
            echo "Unsupported package manager. Please install the required packages manually. See https://quarkus.io/guides/building-native-image"
            exit 1
        fi
    elif [ "$OS" = "Darwin" ]; then
        if ! command -v xcode-select >/dev/null; then
            echo "Xcode is not installed. Installing Xcode...";
            xcode-select --install;
        else
            echo "Xcode is already installed.";
        fi
    else
        echo "Unsupported operating system. Please install the required packages manually."
        exit 1
    fi

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
