# Lists available commands
default:
    @just --list --unsorted --justfile {{justfile()}}

# Brings up chooser, requires fzf to be installed.
choose:
    @just --choose --chooser 'fzf'

# Cleanup all modules
clean:
    ./mvnw clean

# Builds dotCMS
clean-build:
    ./mvnw -DskipTests clean install

# clean build and test
clean-verify:
    ./mvnw clean verify


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
    ./mvnw -pl :dotcms-integration -Pit-tests pre-integration-test -am

dependencies:
    ./mvnw dependency:tree

core-dependencies:
    ./mvnw -pl :dotcms-core dependency:tree


