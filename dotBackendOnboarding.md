# dotCMS Back End Setup

## Get Started

Welcome to the onboarding guide for the dotCMS Core project. This document is designed to help new developers quickly get up to speed with our Java Maven multi-module project hosted on GitHub. By following this guide, you'll learn how to install, configure, compile, and run the project efficiently. Let's get started!

## Prerequisites

Before you begin, ensure you have the following tools installed on your system:

- **Homebrew**: A package manager for macOS that simplifies the installation of software.
	```sh
	/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
	```
- **Git**: A version control system for tracking changes in source code during software development.
	```sh
	brew install git
	```
- **Just**: A command runner for simplifying common tasks with scripts.
	```sh
	brew install just
	```
- IntelliJ IDEA (Recommended): A powerful integrated development environment (IDE) for Java development.
   -   Download and install from [IntelliJ IDEA website](https://www.jetbrains.com/idea/download/).

## Installation

Follow these steps to install and set up the dotCMS Core project:

1. **Clone the repository**:
	```sh
	git clone git@github.com:dotCMS/core.git
	```
2. **Navigate to the project root**:
	```sh
	cd <project-installation-path>
	```
3. **Install dependencies**:
	```sh
	just install-all-mac-deps
	```
4. **Import the project into IntelliJ**:
	- Open IntelliJ IDEA.
	- Select "Open" and navigate to the `pom.xml` file in the `core` directory to import the project.
	
## Setup

Configure the necessary environment settings for the dotCMS Core project:

1. **Create a directory for the license**:
	```sh
	mkdir -p ${user.home}/.dotcms/license
	```		
	```sh
	cp <license-file.dat> ${user.home}/.dotcms/license/
	```	
	> ℹ️ **Note**: Ask for the `license.dat` file if you don't have it.

## Build

To build the project, follow these commands:

1. **Clean and compile all modules (without tests and Docker images)**:
	| Just | Maven |
	|--|--|
	| `just build-no-docker`| `./mvnw clean install -DskipTests -Ddocker.skip`  |
	
2. **Compile only the core module (without tests or Docker images)**:
	| Just | Maven |
	|--|--|
	| `just build-select-module <module>`| `./mvnw install -pl :{{ module }} -DskipTests -Ddocker.skip`  |
	
	> 💡**Tip**: To build *dotcms-core* module you can just run `just build-select-module dotcms-core`
	
## Running

After compiling the project, the next step is to run it. This section provides detailed instructions on how to start the dotCMS Core project.

1. **Start | Stop** the application: We can start the dotCMS application in a Docker container on a dynamic port, running in the background. We have the *stop* action as well.

	| Action | Just | Maven |
	|--|--|--|
	| start | `just dev-start-on-port 8080`| `./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080`  |
	| stop | `just dev-stop`| `./mvnw -pl :dotcms-core -Pdocker-stop`  |
	
	> 💡**Tip**:  If you don't provide any port the command `just dev-start-on-port` will run on port  _**8082**_ by default. You can also start the application up on a random port running the command `just dev-start`.
	
	> ℹ️ **Note**: The project runs in a Docker container by default, so local installation of **PostgreSQL** or **Elasticsearch** is not necessary. However, external configuration of these resources is possible if needed.

## Testing

Testing is a crucial part of the development process to ensure the stability and reliability of the application. In this section, you'll learn how to run **Unit** tests,  **Integration** tests and **Postman** tests of the project. We will cover the commands needed to execute these tests and provide guidance on how to interpret the results. Proper testing helps identify and fix issues early, ensuring a more robust and error-free application.

1. **Integration** and **Postman** tests

	| Type | Just | Maven |
	|--|--|--|
	| all | `just build-test-full` | `./mvnw clean install -Dcoreit.test.skip=false -Dpostman.test.skip=false` |
	| test-integration | `just test-integration`| `./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false`  |
	| test-postman | `just test-postman <collections>`| `./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false -Pdebug -Dpostman.collections={{ collections }}`  |

	> ⚠️ Note that the command `just build-test-full` can consume a lot of machine resources and may take a long time to be completed. Its use is more recommended in the _**cloud environment**_.

	> 💡**Tip**:  If you don't provide any collections the command `just test-postman` will run **page** collection tests by default.

## Debugging

Debugging is essential for diagnosing and resolving issues that arise during development. This section will guide you through the debugging process using IntelliJ IDEA. 

With the latest just file changes introduced, there is an easier way to debug **dotCMS**, and also you can run IT using the IDE. Please be aware that since everything is running in Docker, you might need to keep an eye on the containers, volumes and images in Docker Hub.

### Debug mode

It will start dotCMS debug mode without any other configuration required, and the port debug will be **_5005_**. All will be running in **Docker** (**Elasticsearch**, **PostgresSQL** and **dotCMS**).

1. **First, we need to build the project**:
	| Action | Just | Maven |
	|--|--|--|
	| build | `just build` | `./mvnw -DskipTests clean install`

2. **And then run this one**:
	| Action | Just | Maven |
	|--|--|--|
	| Startup | `just dev-run-debug-suspend` | `./mvnw  -pl  :dotcms-core  -Pdocker-start,debug-suspend`

### Integration tests using IDE

1. **First, we need to build the project**:
	| Action | Just | Maven |
	|--|--|--|
	| Build | `just build` | `./mvnw -DskipTests clean install`

2. **Then, we need to start up the following service**:
	| Action | Just | Maven |
	|--|--|--|
	| Start | `just test-integration-ide` | `./mvnw -pl :dotcms-integration pre-integration-test -Dcoreit.test.skip=false`

	> ℹ️ **Note:** This command prepares  the  environment  for  running  integration  tests  in  an  IDE.

3. After that, you can click **Debug / Run** directly on the IDE for the test class you want to.
4. **Once testing is finished**, you can stop the test integration services through the following command:
	| Action | Just | Maven |
	|--|--|--|
	| Stop | `just test-integration-stop` | `./mvnw -pl :dotcms-integration -Pdocker-stop -Dcoreit.test.skip=false`

## Advanced Configurations

Advanced configurations allow you to customize and optimize your development environment and application settings. This section covers configuring external resources like **PostgreSQL** and **Elasticsearch**, customizing Maven build options, and adjusting **Docker** settings. By tailoring these configurations to your specific needs, you can enhance performance, improve security, and streamline your development workflow.`

### dotCMS application settings

**Configure initial admin password**:

* Set up the following env variables in the `~/.zshrc` file.
	```sh
	export DOT_INITIAL_ADMIN_PASSWORD=admin
	```
**Using a full starter**:

* If you need to start up your instance using a **Full Starter**, just go to this file: **_core/parent/pom.xml_** , and update the `<starter.deploy.version>` property with the starter you need.

	**_Example_**:
	```xml
	<starter.deploy.version>20231117</starter.deploy.version>
	```
* Alternatively, you can go directly to the following folder: **_core/dotCMS/target/starter/_** , and drop the starter’s ZIP file in there. Just make sure you rename it to **starter.zip**, as usual.

* Another way is to set up the following environment variable in the `~/.zshrc` file:
	```sh
	DOT_STARTER_DATA_LOAD =	'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20240213/starter-20240213.zip'
	```
	Finally, once the change is made (using one of the previous options), remember to **recompile** the code so that the changes will take effect, running the following command:
	
	| Just | Maven |
	|--|--|
	| `just build-no-docker`| `./mvnw clean install -DskipTests -Ddocker.skip`  |	

~~**Upload a new starter**:~~
1. Create a **_mysettings.xml_** file (can be placed in any location):
	```xml
	<settings>
		<servers>
			<server>
				<id>dotcms-libs</id>
				<username>{USERNAME}</username>
				<password>{PASSWORD}</password>
			</server>
		</servers>
	</settings>	
	```
2. Then, **run** the following command: 
	```sh
	./mvnw deploy:deploy-file -Durl=https://repo.dotcms.com/artifactory/libs-release-local -DrepositoryId=dotcms-libs -Dfile={PATH_OF_THE_ZIP} -DgroupId=com.dotcms -DartifactId=starter -Dversion={{empty_}timestamp} -Dpackaging=zip -s {PATH}/mysettings.xml -pl :dotcms-core
	```
	**_Example_**:
	```sh
	./mvnw  deploy:deploy-file  -Durl=[https://repo.dotcms.com/artifactory/libs-release-local](https://repo.dotcms.com/artifactory/libs-release-local)  -DrepositoryId=dotcms-libs  -Dfile=/Users/<user>/Desktop/empty_20240131.zip  -DgroupId=com.dotcms  -DartifactId=starter  -Dversion=empty_20240131  -Dpackaging=zip  -s  /Users/<user>/Documents/dotcms_master_maven/mysettings.xml  -pl  :dotcms-core
	```
3. Update the **_parent/pom.xml_** file with the new starter version:
	```xml
	<starter.deploy.version>{NEW_STARTER}</starter.deploy.version>
	```

### Local DB and ES settings

**Configure a local PostgresSQL**:

* Set up the following env variables in the `~/.zshrc` file.
	```sh
	export DB_BASE_URL=jdbc:postgresql://localhost/dotcms
	export DB_DRIVER=org.postgresql.Driver
	export DB_PASSWORD=XXXXXXXXXX
	export DB_USERNAME=dotcmsdbuser
	export DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS=com.dotmarketing.db.SystemEnvDataSourceStrategy
	```
**Configure a local Elasticsearch**:

* Set up the following env variables in the `~/.zshrc` file.
	```sh
	export DOT_ES_ENDPOINTS=[https://localhost:9200](https://localhost:9200)
	export DOT_ES_AUTH_BASIC_PASSWORD=admin
	export DOT_ES_AUTH_BASIC_USER=admin
	export DOT_ES_AUTH_TYPE=BASIC
	```

## Resources and Further Reading
For more advanced actions and commands, you can consult the `justfile` in the project repository. The `justfile` contains various scripts that simplify common tasks and automate complex processes, making it a valuable reference for efficient development workflows.
