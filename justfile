# Lists available commands
default:
    @just --list --unsorted --justfile {{justfile()}}

# Brings up chooser, requires fzf to be installed.
choose:
    @just --choose --chooser 'fzf'

# Builds dotCMS
build:
    ./mvnw -DskipTests clean install

# Build and install modules to the local repo
quick-install:
    ./mvnw -DskipTests install

# Runs the tests
test-build:
    ./mvnw clean test

full-build-test:
    ./mvnw -Pit-tests install

# Prepare integration tests for running in IDE
prepare-it-test:
    ./mvnw -pl :dotcms-integration -Pit-tests pre-integration-test

dependencies:
    ./mvnw dependency:tree

core-dependencies:
    ./mvnw -pl :dotcms-core dependency:tree


