# dotCMS Frontend Onboarding Guide

## Get Started

Welcome to the frontend onboarding guide. This document will help you get set up quickly and efficiently, providing all the necessary information to start contributing to the project.

> **📚 Important**: After completing this onboarding, refer to the comprehensive documentation in the `/docs/` directory for detailed development patterns, coding standards, and architectural guidance. Start with [Angular Standards](docs/frontend/ANGULAR_STANDARDS.md) and [Testing Frontend](docs/frontend/TESTING_FRONTEND.md) for frontend development.

### Tech Stack Overview
- **Angular**: 18.2.3 with standalone components and signals
- **UI Components**: PrimeNG 17.18.11, PrimeFlex 3.3.1  
- **State Management**: NgRx Signals, Component Store
- **Build Tool**: Nx 19.6.5
- **Testing**: Jest + Spectator (required) 

## Prerequisites

Before you begin, ensure you have the following tools installed on your system:

- **Homebrew**: Package manager for macOS
  ```sh
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  ```

- **Git**: Version control system  
  ```sh
  brew install git
  ```

- **NVM**: Node Version Manager
  ```sh
  brew install nvm
  ```  

- **Node**: JavaScript runtime environment
  ```sh
  nvm install 18.18.2
  ```

- **Nx**: Monorepo tool
  ```sh
  npm install -g nx@latest
  ```

- **Yarn**: Alternative package manager
  ```sh
  brew install yarn
  ```

- **VS Code**: Code editor for development
  - Download from [VS Code website](https://code.visualstudio.com/download) (Recommended)

- **Nx Console**: A Visual Studio Code extension for Nx
  - Download from [Vs Code Marketplace ](https://marketplace.visualstudio.com/items?itemName=nrwl.angular-console) (Optional)


## Installation

Follow these steps to install and set up the frontend project. This section will guide you through the process of cloning the repository, installing necessary dependencies, and preparing your development environment. By the end of this section, you will have a local instance of the project running on your machine, ready for development.

1. **Clone the repository**:
   First, you need to get a copy of the project repository on your local machine. Use the following command to clone the repository from GitHub:
   ```sh
   git clone git@github.com:dotCMS/core.git
   ```

2. **Navigate to the project root**:
  After cloning the repository, navigate to the project directory. Replace `<path-to-project>` with the actual path to the directory where you cloned the repository:
    ```sh
    cd <path-to-project>/
    ```
3. **Install dependencies**: _**Just**_ is a command runner for simplifying common tasks with scripts. 
	```sh
	brew install just
	```

	```sh
	just install-all-mac-deps
	```

	> ℹ️ **Note**: This command ensures that all necessary dependencies such as **Git**, **JDK**, and **Docker** are installed. If any dependencies are missing, this step will handle their installation.

## Running

To run the frontend application locally, follow these steps. This section will guide you through building the project, starting the necessary backend services, and running the frontend application.

1. **Build the project**:
   First, you need to install the project dependencies and build the project. Use the following command:

   ```sh
   yarn install
   ``` 
   This will install all necessary dependencies and prepare the project for development.

2. **Start backend services**:
  Before starting the frontend application, ensure that all necessary backend services are running. These services are defined in a **Docker Compose** file located in the **core-web** directory. The backend services will be proxied to the frontend application through configurations defined in the `proxy-dev.conf.mjs` file. 
  
    Use the following command to start the backend services required by the frontend application:

    ```sh
    docker-compose -f docker/docker-compose-examples/single-node/docker-compose.yml up
    ```

3. **Run the frontend application**:
  Once the backend services are running, you can start the frontend application using the following command:

    ```sh
    nx run dotcms-ui:serve
    ```
    The frontend will be available at the following URL:

    ```sh
    http://localhost:4200/dotAdmin
    ```
    By following these steps, you will have the frontend application running on your local machine, ready for development and testing.

## Testing

Testing is crucial for ensuring code quality and reliability. The dotCMS frontend uses **Jest + Spectator** for comprehensive testing.

### Essential Testing Commands

1. **Run Unit Tests for Specific Component**:
   ```bash
   # Run tests for specific component (recommended during development)
   cd core-web && nx run dotcms-ui:test --testNamePattern="MyComponent"
   ```

2. **Run All Unit Tests**:
   ```bash
   # Run all unit tests for dotcms-ui
   cd core-web && nx run dotcms-ui:test
   ```

3. **Run Affected Tests**:
   ```bash
   # Run tests only for files changed since last commit
   cd core-web && nx affected -t test --exclude='tag:skip:test'
   ```

4. **Lint Code**:
   ```bash
   # Check code quality and style
   cd core-web && nx run dotcms-ui:lint
   ```

### Testing Standards (Critical)

**ALWAYS follow these patterns when writing tests:**

```typescript
// ✅ ALWAYS use data-testid for element selection
const button = spectator.query(byTestId('submit-button'));

// ✅ ALWAYS use spectator.setInput() for component inputs  
spectator.setInput('inputProperty', 'value');
// NEVER: spectator.component.inputProperty = 'value';

// ✅ Test user interactions, not implementation details
spectator.click(byTestId('save-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();
```

> **📚 Next Steps**: After onboarding, see the comprehensive documentation in `/docs/frontend/` for detailed Angular development patterns, testing best practices with Spectator, component architecture guidelines, and styling standards.  

## Advanced Configurations

In this section, you'll find advanced configuration options to optimize your development workflow and customize your environment. These configurations include how to insert the dotCMS license, map the Tomcat root directory for working with JSPs without recompiling and rebuilding the backend, overwrite language files, and enable application features through environment variables and feature flags.

### Setting the dotCMS License

- To set and activate your dotCMS license key to unlock full functionality of the platform, you need to map the license file in the `docker-compose` configuration. Add the volume mapping for the license file in the `volumes` section of your `docker-compose` file as shown below:

  ```yaml
  volumes:
    - cms-shared:/data/shared
    # MOUNT THE FOLLOWING LICENSE FILE
    - ./your-dotCMS-license.zip:/data/shared/assets/license.zip
  ```  

### Mapping the Tomcat Root Directory

- Mapping the Tomcat root directory allows you to work with JSP files directly without the need to recompile, rebuild, and redeploy the backend each time you make changes. This setup significantly speeds up the development process. To achieve this, add a new `volume` in your `docker-compose` file as shown below:

  ```yaml
  volumes:
    - cms-shared:/data/shared
    # MOUNT THE FOLLOWING FOLDER TO THE ROOT OF THE TOMCAT
    - <project-path>/core/dotCMS/src/main/webapp/html:/srv/dotserver/tomcat-9.0.85/webapps/ROOT/html
  ```

### Overwriting Language Files

- To overwrite language files and customize the application's localization without modifying the original source files, you can map a new `volume` in your `docker-compose` file as follows:

  ```yaml
  volumes:
    - cms-shared:/data/shared
    # OVERWRITE LANGUAGE PROPERTIES FILES
    - <project-path>/core/dotCMS/src/main/webapp/WEB-INF/messages:/srv/dotserver/tomcat-9.0.85/webapps/ROOT/WEB-INF/messages
  ```

  This configuration maps the local folder containing your customized language properties files to the appropriate directory within the Tomcat web server, allowing for easy updates and maintenance of language-specific content.

### Environment variables and feature flags

- You can enable and manage various features of the application using environment variables and feature flags. To do this, add a new environment variable in your `docker-compose` file as shown below:

  ```yaml
  services:
    dotcms:
      image: dotcms/dotcms:trunk
      environment:
        ...
        # ENABLE THE FOLLOWING FEATURES
        DOT_FEATURE_FLAG_SEO_IMPROVEMENTS: true
        DOT_FEATURE_FLAG_SEO_PAGE_TOOLS: true
        DOT_FEATURE_FLAG_NEW_BINARY_FIELD: true        
        ...
  ```

  Each feature has its own specific environment variable. To know which features can be enabled through feature flags, you need to consult with the team.

By leveraging these advanced configurations, you can streamline your development process, customize your environment, and enhance your productivity.

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