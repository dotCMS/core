# dotCMS CLI 


## Installing the dotCMS CLI
The dotCMS CLI is delivered as an uber jar that can be downloaded from here.
 Once downloaded, you just need to run it with: 

```shell script
java -jar dotcms-cli.jar
```

## Using the dotCMS CLI from a docker installation
In case you are using dotCMS installed in docker, you can run the CLI directly from the docker container with:
```shell script
TODO: include here the command to be executed
```

## Available commands
| Command                                    | Description                                                                      |
|--------------------------------------------|----------------------------------------------------------------------------------|
| [content-type](cli/docs/content-type.adoc) | Performs operations over content types. For example: pull, push, remove          |
| [files](cli/docs/files.adoc)               | Performs operations over files. For example: tree, ls, push                      |
| [instance](cli/docs/instance.adoc)         | Prints a list of available dotCMS instances                                      |
| [language](cli/docs/language.adoc)         | Performs operations over languages. For example: pull, push, remove              |
| [login](cli/docs/login.adoc)               | Logs into a dotCMS instance                                                      |
| [site](cli/docs/site.adoc)                 | Performs operations over sites. For example: pull, push, remove                  |
| [status](cli/docs/status.adoc)             | Provides information about the current logged-in user and dotCMS instance status |


You can find more details about how to use the dotCMS CLI in the [Examples](#examples) section.



## Examples

1. Log in with an admin user
```shell script
login --user=admin@dotCMS.com --password=admin
```
2. List and choose the dotCMS instance we want to run against
```shell script
instance --list
```
3. 


## Building the CLI (dev mode)

The CLI is a quarkus/pico-cli project made up of two modules [cli](cli) and [api-data-module](api-data-model).
This project acts as a maven reactor pom build. You can build at the top level to build the modules.

```shell script
./mvnw clean install -DskipTests=true
```

We suggest to build the project ignoring test execution (`-DskipTests=true`). It will run faster and avoid setting up a testing environment.

#### Running the CLI (dev mode)

You might use [quarkus cli](https://es.quarkus.io/guides/cli-tooling) or [maven](https://maven.apache.org/install.html)
First, start a dotCMS instance locally. 
Then, execute the following commands

```shell script
# from top level to build all
cd cli
# command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
quarkus dev
```
**NOTE:**  To reduce duplication in the multi-module project, mvnw is not included on each submodule.
The quarkus command finds the executable

To run mvnw from a submodule just use a relative path to the parent mvn.
If running from a submodule folder, all the dependencies will need to be up to date and installed to the local mvn with maven install

Alternatively, you can specify the subproject from the parent folder
```shell script
# from top level to build all
cd cli
# The command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
../mvnw -pl cli quarkus:dev
```

Once the cli is launched in dev mode it'll print out a list of available commands.

followed by 

```shell script
--
Tests paused
Press [space] to restart, [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```

We can also instruct Quarkus dev mode to launch our cli using a preconfigured param by doing:
```shell script
../mvn quarkus:dev -Dquarkus.args=status
```
This will launch the cli passing directly into it the arguments that tell them to execute the command status.

## Building a CLI jar 
In order to generate the cli as a jar packaged with all necessary dependencies you need to run the following command from the `cli` directory:
```shell script
../mvnw clean install package
```

All the commands can be executed directly from the generated jar which can be found under `cli/target/quarkus-app/`.

Example:
```shell script
java -jar ./cli/target/quarkus-app/quarkus-run.jar status
```

## Logging

When running the CLI, a **_dotcms-cli.log_** file will be created in the directory where the CLI
executable is run.

#### File log level

To increase the file log level to _DEBUG_ when running in dev mode, use the following command:

```shell
../mvnw quarkus:dev -Dquarkus.log.file.level=DEBUG
```

#### Console log level

To increase the console log level to _DEBUG_ when running in dev mode, use the following command:

```shell
../mvnw quarkus:dev -Dquarkus.log.handler.console.\"DOTCMS_CONSOLE\".level=DEBUG
```

#### File log location

To override the default location of the log file, you have two options:

##### 1. Set the environment variable

Example:

```shell
export QUARKUS_LOG_FILE_PATH=/Users/my-user/CLI/dotcms-cli.log
java -jar cli-1.0.0-SNAPSHOT-runner.jar login -u admin@dotcms.com -p admin
```

##### 2. Set the system property

Example:

```shell
../mvnw quarkus:dev -Dquarkus.log.file.path=/Users/my-user/CLI/dotcms-cli.log
```
