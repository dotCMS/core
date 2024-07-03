# dotCMS Front End Setup

## Get Started

Welcome to the frontend onboarding guide. This document will help you get set up quickly and efficiently, providing all the necessary information to start contributing to the project. 

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
  brew install node
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

- **Just**: A command runner for simplifying common tasks with scripts.
	```sh
	brew install just
	```

- **VS Code**: Code editor for development
  - Download from [VS Code website](https://code.visualstudio.com/download) (Recommended)


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
3. **Install dependencies**:
	```sh
	just install-all-mac-deps
	```

## Setup

Before running the project, you need to configure the necessary environment settings for the frontend project.

* Export the environment variable `NPM_TOKEN` to configure the frontend project with your NPM token. If you don't have one, you can create one by following the steps in the [NPM documentation](https://docs.npmjs.com/creating-and-viewing-authentication-tokens).

  ```sh
  export NPM_TOKEN=<your-npm-token>
  ```

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
    docker-compose -f core-web/docker-compose.yml up
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

Ensuring the quality and reliability of the code is crucial in any development process. This section provides the necessary commands and steps to run tests, lint the code, and perform continuous integration tests for the **dotCMS** project. 

The following commands are essential for testing the frontend application:

- **Run Unit Tests for dotCMS UI**:
  ```sh
  nx run dotcms-ui:test
  ```

- **Run Affected Tests for dotCMS**:
This command runs tests for the affected parts of the project, excluding those tagged to skip tests.

  ```sh
  nx affected -t test --exclude='tag:skip:test'
  ```

- **Lint dotCMS**:
Linting helps in maintaining code quality by checking for syntax and style issues.

  ```sh
  nx run-many -t test --exclude='tag:skip:lint'
  ```

- **Continuous Integration Tests for dotCMS**:
This command runs tests in the CI environment.

  ```sh
  ../mvnw -pl :dotcms-core-web test
  ```

By utilizing these commands, you can effectively manage and ensure the quality of the codebase through thorough testing and linting.  

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
      image: dotcms/dotcms:trunk_149bea9
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

## Resources and Further Reading
Below, you'll find a curated list of resources to help deepen your understanding of the technologies and tools used in our project. We recommend that you familiarize yourself with these resources before diving into the project.

- [DotCMS Documentation](https://dotcms.com/docs/)
- [DotCMS R&D - Onboarding](https://docs.google.com/document/d/1BKwGbqyVNBjc_FuP6tD2R9Aqloh28b-2nstlnziLDRw/edit#heading=h.34zjomn5aq0w)