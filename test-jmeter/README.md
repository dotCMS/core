# dotCMS JMeter Performance Tests

This module contains JMeter performance tests for dotCMS. The tests are configured to run against a dotCMS instance and measure various performance metrics.

This is a work in progress.  It currently requires a https connection to the dotCMS instance to maintain session cookies.  It will also not run in the CI environment and is only for local testing requiring the -Djmeter.test.skip=false flag to be set to enable


## Test Configuration

The JMeter tests are configured in `src/test/jmeter/sessions.jmx`. The default configuration includes:

- Host: dotcms.local
- Port: 443
- Ramp-up period: 0 seconds
- Startup delay: 5 seconds
- Test duration: 5 seconds

## Running the Tests

### Basic Execution

```bash
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter
```

you can also run the above with the justfile alias `run-jmeter-tests`:


```bash
just run-jmeter-tests
```

### Opening test script in JMeter GUI

```bash
cd test-jmeter
../mvnw jmeter:configure jmeter:gui -DguiTestFile=src/test/jmeter/sessions.jmx
````

### Overriding Test Parameters

You can override the default settings using command-line properties:

```bash
# Override host and port
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=my-dotcms-instance.com \
    -Djmeter.port=444 \ # default is 443
    -Djmeter.thread.number=10 # The number of concurrent users to simulate

# Override test timing parameters
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.rampup=10 \
    -Djmeter.startup.delay=2 \
    -Djmeter.test.duration=30
```

## Test Reports

HTML reports are generated in the `target/jmeter/reports` directory. The plugin is configured to:
- Delete existing results before new test runs

A csv is also generated in the `target/jmeter/results` directory. e.g. `20241203-sessions.csv` this contains
- Capture additional variables: JVM_HOSTNAME, SESSION_ID, X_DOT_SERVER
- The SESSION_ID and X_DOT_SERVER can be used in the csv to validate session propagation when there are multiple replicas and can be used to show behaviour when replicas are scaled up and down.
## Configuration Files

- Main JMeter test file: `src/test/jmeter/sessions.jmx`
- Maven configuration: `pom.xml`

## Properties Configuration

Default properties in pom.xml:
```xml
<properties>
    <jmeter.host>dotcms.local</jmeter.host>
    <jmeter.port>443</jmeter.port>
    <jmeter.rampup>0</jmeter.rampup>
    <jmeter.startup.delay>5</jmeter.startup.delay>
    <jmeter.test.duration>60</jmeter.test.duration>
    <jmeter.thread.number>2</jmeter.thread.number>
</properties>
```

## Profile Information

The tests run under the `jmeter-standalone` profile, which is active by default. This profile includes:
- Clean configuration for test reports
- JMeter test execution
- Results validation
- Report generation


# env password
When connecting to an external instance, to prevent the need to add the password to the command line, the password can be added to the environment variable JMETER_ADMIN_PASSWORD.  
To prevent unexpected use of this you must add the property -Djmeter.env.password=true  or set the profile -Pjmeter.env.password
```bash
export JMETER_ADMIN_PASSWORD=mysecretpassword
./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter -Djmeter.host=myhost -Djmeter.env.password=true
```

## Troubleshooting

We have not yet validated the memory requirements.  Eventually we should probably be explicit about the JVM memory settings.  These can be added into the configuration block 
in the pom.xml e.g.:
```xml
<jMeterProcessJVMSettings>
    <arguments>
        <argument>-XX:MaxMetaspaceSize=256m</argument>
        <argument>-Xmx1024m</argument>
        <argument>-Xms1024m</argument>
    </arguments>
</jMeterProcessJVMSettings>
```
## High load testing
Currently this runs as a standalone service, for high load testing we would need to run this in a distributed mode with multiple jmeter instances and jmeter should not be running on the same server as DotCMS.  This is not yet supported.
As such performance issues in adding too many threads may be due to local server limitations of resources and not the dotCMS instance itself. 
