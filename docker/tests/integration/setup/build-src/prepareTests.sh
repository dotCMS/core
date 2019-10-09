#!/bin/bash

# Prepare everything to run the integration tests

# We will run some heavy compile tasks (and download dependencies) in order to take
# advantage of the build image cache
cd /build/src/core/dotCMS \
    && ./gradlew compileIntegrationTestJava \
    && ./gradlew prepareIntegrationTests