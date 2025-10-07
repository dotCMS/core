# dotCMS Backend Onboarding

## Get Started

Welcome to the onboarding guide for the dotCMS Core project. This document is designed to help new developers quickly get up to speed with our Java Maven multi-module project hosted on GitHub. By following this guide, you'll learn how to install, configure, compile, and run the project efficiently.

> **📚 Important**: After completing this onboarding, refer to the comprehensive documentation in the `/docs/` directory for detailed development patterns, coding standards, and architectural guidance. Start with [Java Standards](docs/backend/JAVA_STANDARDS.md) and [Maven Build System](docs/backend/MAVEN_BUILD_SYSTEM.md) for backend development.

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

Testing is crucial for ensuring stability and reliability. **IMPORTANT**: Never run full integration test suites during development (60+ minutes). Always target specific test classes or methods.

### Efficient Testing Strategy

1. **Targeted Integration Tests (RECOMMENDED - 2-10 minutes)**:
	```bash
	# Target specific test class
	./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest
	
	# Target specific test method  
	./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest#testMethod
	```

2. **IDE Testing Workflow (FASTEST - 10-30 seconds per test)**:
	```bash
	# Start services for IDE debugging
	just test-integration-ide
	# → Now run/debug individual tests in your IDE with breakpoints
	just test-integration-stop  # Clean up when done
	```

3. **Postman Tests**:
	```bash
	# Specific collection (recommended)
	just test-postman ai                                    # AI collection only
	./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false -Dpostman.collections=ai
	
	# All postman tests (slower)
	just test-postman all
	```

4. **Full Test Suite (CI/Cloud Only - 60+ minutes)**:
	```bash
	# ⚠️ WARNING: Only for final validation or CI environment
	just build-test-full
	./mvnw clean install -Dcoreit.test.skip=false -Dpostman.test.skip=false
	```

> **📚 Next Steps**: After onboarding, see the comprehensive documentation in `/docs/backend/` for detailed Java development patterns, Maven configuration, REST API design, and database access patterns.

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

* Set up the following env variables:
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

* Another way is to set up the following environment variable:
	```sh
	DOT_STARTER_DATA_LOAD =	'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20240213/starter-20240213.zip'
	```
	Finally, once the change is made (using one of the previous options), remember to **recompile** the code so that the changes will take effect, running the following command:
	
	| Just | Maven |
	|--|--|
	| `just build-no-docker`| `./mvnw clean install -DskipTests -Ddocker.skip`  |	


## Troubleshooting

In this section, we address common issues you might encounter during setup, development, and deployment, along with their solutions. Following these steps will help you quickly resolve problems and continue with your development tasks smoothly.

### Puppeteer Chromium Binary Not Available for ARM64

You might encounter the following error when setting up Puppeteer on an ARM64-based machine (such as Apple's M1/M2 Macs):

```sh
[INFO] Directory: /Users/<user>/Documents/projects/dotcms/sourcecode/core/core-web/node_modules/puppeteer
[INFO] Output:
[INFO] The chromium binary is not available for arm64:
[INFO] If you are on Ubuntu, you can install with:
[INFO]
[INFO] apt-get install chromium-browser
[INFO]
[INFO] /Users/<user>/Documents/projects/dotcms/sourcecode/core/core-web/node_modules/puppeteer/lib/cjs/puppeteer/node/BrowserFetcher.js:112
[INFO] throw new Error();
[INFO] ^
[INFO]
[INFO] Error
[INFO] at /Users/<user>/Documents/projects/dotcms/sourcecode/core/core-web/node_modules/puppeteer/lib/cjs/puppeteer/node/BrowserFetcher.js:112:19
[INFO] at FSReqCallback.oncomplete (node:fs:210:21)
[INFO]
[INFO] Node.js v18.18.2
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for dotcms-root 1.0.0-SNAPSHOT:
[INFO]
[INFO] dotcms-parent ...................................... SUCCESS [ 0.335 s]
[INFO] dotcms-independent-projects ........................ SUCCESS [ 0.023 s]
[INFO] dotcms-core-plugins-parent ......................... SUCCESS [ 0.021 s]
[INFO] com.dotcms.tika-api ................................ SUCCESS [ 0.538 s]
[INFO] com.dotcms.tika .................................... SUCCESS [ 5.262 s]
[INFO] dotcms-bom ......................................... SUCCESS [ 0.021 s]
[INFO] dotcms-logging-bom ................................. SUCCESS [ 0.020 s]
[INFO] dotcms-application-bom ............................. SUCCESS [ 0.027 s]
[INFO] dotcms-nodejs-parent ............................... SUCCESS [ 5.478 s]
[INFO] dotcms-core-web .................................... FAILURE [02:11 min]
[INFO] dotcms-osgi-base ................................... SKIPPED
[INFO] dotcms-core-bundles ................................ SKIPPED
[INFO] dotcms-system-bundles .............................. SKIPPED
[INFO] dotcms-build-parent ................................ SKIPPED
[INFO] dotcms-core ........................................ SKIPPED
[INFO] dotcms-cli-parent .................................. SKIPPED
[INFO] dotcms-api-data-model .............................. SKIPPED
[INFO] dotcms-cli ......................................... SKIPPED
[INFO] dotcms-integration ................................. SKIPPED
[INFO] dotcms-postman ..................................... SKIPPED
[INFO] dotcms-reports ..................................... SKIPPED
[INFO] dotcms-root ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 02:24 min
[INFO] Finished at: 2024-06-05T14:35:40-05:00
[INFO] ------------------------------------------------------------------------
```


This error occurs because Puppeteer attempts to download a Chromium binary that is not compatible with the ARM64 architecture by default. This issue can arise due to several reasons, including:

- **New Installation on ARM64 Machines**: Setting up a new development environment on ARM64-based machines.
- **Puppeteer Version Compatibility**: Using a version of Puppeteer that does not fully support ARM64 architecture.
- **Environment Variable Misconfiguration**: Incorrect `PUPPETEER_EXECUTABLE_PATH` environment variable setting.
- **Network Issues**: Restrictions that prevent Puppeteer from downloading the Chromium binary.
- **Custom Docker Images**: Improperly configured custom Docker images for ARM64 architectures.
- **Cross-Platform Development**: Working on multiple platforms without the correct configuration.

#### Solution

To resolve this issue, follow the steps documented in this guide: [How to fix M1 Mac Puppeteer Chromium ARM64 bug](https://linguinecode.com/post/how-to-fix-m1-mac-puppeteer-chromium-arm64-bug).

1. **Install Chromium for ARM64**:
  First, ensure you have the necessary tools installed. You can use Homebrew to install the ARM64 version of Chromium.

    ```sh
    brew install chromium
    ```
2. **Set the Puppeteer Executable Path**:
  Configure Puppeteer to use the installed Chromium binary by setting the `PUPPETEER_EXECUTABLE_PATH` environment variable. Add the following lines to your `.bashrc`, `.zshrc`, or appropriate shell configuration file:

    ```sh
    export PUPPETEER_EXECUTABLE_PATH=$(which chromium)
    ```

3. **Source the Configuration**:
  Reload your shell configuration to apply the changes:

    ```sh
    source ~/.zshrc  # or ~/.bashrc, depending on your shell
    ```
4. **Configure Puppeteer to Skip Downloading Chromium**:
  To avoid downloading Chromium in the future, set the `PUPPETEER_SKIP_CHROMIUM_DOWNLOAD` environment variable:

    ```sh
    export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
    ```

    Add this to your shell configuration file to make it persistent:

    ```sh
    echo "export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true" >> ~/.zshrc  # or ~/.bashrc
    ```

5. **Reinstall Puppeteer**:
  Finally, reinstall Puppeteer to ensure it picks up the correct Chromium binary and respects the skip download setting:

    ```sh
    npm install puppeteer
    ```

By following these steps, you should be able to resolve the Puppeteer Chromium binary issue and prevent it from occurring in the future, allowing you to proceed with your development tasks.

## dotCMS Utilities

**dotCMS Utilities** is a collection of internal tools designed to streamline various Git-related tasks and processes within our development workflow. These utilities help developers by automating tasks such as creating new branches, switching contexts, and creating pull requests.

### How to Use dotCMS Utilities

1. **Installation**:
  To install [dotCMS Utilities](https://github.com/dotCMS/dotcms-utilities), run the following `curl` command:

	```sh
	curl -sSL https://raw.githubusercontent.com/dotCMS/dotcms-utilities/main/install.sh | bash
	```
2. **Basic Commands/Usage**:
	Once installed, you can use the utilities for managing your Git workflow. Here are some examples of how to use the utilities:

	#### Switch to a different branch
	This Git extension script, git issue-branch, helps select an issue from the list assigned to the current user and creates a new branch for that issue. The branch will be prefixed automatically with the issue id and a default suffix will be created based upon the issue title. The user can specify their own suffix also.

	Provides an interactive selection menu for branches.

	```sh
	git switch-branch <branch-name>
	```

	#### Create a new branch
	This Git extension script, git smart-switch, enhances branch switching and creation by providing additional features like interactive branch selection, WIP commit management, and optional remote branch pushing.

	Creates and switches to a new branch based on issue number and title and ensures consistent branch naming.
	
	```sh
	git create-branch <branch-name>
	```

	#### Create a pull request
	This Git extension script, git issue-pr, helps to create a new PR from the command line and relate it to the issue id defined on the current branch.

	Creates a pull request from the current branch and integrates with GitHub CLI to fetch issue details.

	```sh
	git create-pull-request
	```

## Resources and Further Reading
For more advanced actions and commands, you can consult the `justfile` in the project repository. The `justfile` contains several scripts that simplify common tasks and automate complex processes, making it a valuable reference for efficient development workflows.
