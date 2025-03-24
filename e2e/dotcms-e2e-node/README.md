# dotCMS End-to-End Testing Guide

This guide provides comprehensive information about running, writing, and debugging End-to-End (E2E) tests for dotCMS.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Tools and Frameworks](#tools-and-frameworks)
- [Installation](#installation)
- [Running E2E Tests](#running-e2e-tests)
  - [Using Maven](#using-maven)
  - [Using Node.js Directly](#using-nodejs-directly)
  - [Available Scripts](#available-scripts)
- [Debugging Tests](#debugging-tests)
- [Writing Tests](#writing-tests)
  - [Using POM Pattern](#using-pom-pattern)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before running the E2E tests, ensure you have the following installed:

- Java JDK 11 or higher
- Maven
- Node.js (LTS version recommended)
- npm or yarn
- Docker and Docker Compose
- Playwright

## Tools and Frameworks

Our E2E testing suite uses:
- **Playwright**: Main testing framework for browser automation
- **TypeScript**: Programming language for test writing
- **Maven**: Build and dependency management
- **Docker**: Container management for test dependencies

## Installation

1. Clone the repository and navigate to the E2E directory:
```bash
cd e2e/dotcms-e2e-node
```

2. Install Node.js dependencies:
```bash
yarn install --frozen-lockfile
```

3. Install Playwright and its dependencies:
```bash
yarn global add playwright
yarn playwright install-deps
```

## Running E2E Tests

### Using Maven

Maven handles the complete lifecycle, including starting/stopping dependencies automatically.

1. Run the entire test suite:
```bash
./mvnw -pl :dotcms-e2e-node verify -De2e.test.skip=false
```

2. Run specific test files:
```bash
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.specific=login.spec.ts
```

3. Run tests from specific directories:
```bash
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.specific="frontend/tests/login/ frontend/tests/contentSearch/"
```

4. Stop E2E dependencies:
```bash
./mvnw -pl :dotcms-e2e-node -Pdocker-stop -De2e.test.skip=false
```

### Using Node.js Directly

For frontend developers actively working on features:

1. Start dotCMS instance (required):
   - Ensure dotCMS is running on port 8080
   - Use appropriate docker-compose configuration

2. Available scripts:
```bash
# Run against localhost:8080
yarn run start-local

# Run against localhost:4200 (with nx serve)
yarn run start-dev

# Run in headless mode (CI environment)
yarn run start-ci

# Run specific test
yarn run start-dev login.spec.ts

# Run tests from specific folders
yarn run start-dev tests/login tests/contentSearch
```

### Available Scripts

The project includes several npm/yarn scripts to help with development, testing, and maintenance:

#### Test Execution
```bash
# Run tests in local environment
yarn run start-local

# Run tests in development environment
yarn run start-dev

# Run tests in CI environment
yarn run start-ci

# Generate test code using Playwright's codegen
yarn run codegen

# Show test report (automatically runs after tests in non-CI environments)
yarn run show-report
```

#### UI Mode Testing
```bash
# Run UI mode in local environment
yarn run ui:local

# Run UI mode in development environment
yarn run ui:dev

# Run UI mode in CI environment
yarn run ui:ci
```

#### Code Quality Tools
```bash
# Format code using Prettier
yarn run format

# Run ESLint
yarn run lint

# Fix ESLint issues automatically
yarn run lint:fix

# Type check TypeScript code
yarn run ts:check
```

#### Additional Scripts
```bash
# Post-testing operations (generates JUnit reports)
yarn run post-testing
```

Each script serves a specific purpose:
- **start-{env}**: Runs tests in different environments (local/dev/ci)
- **ui:{env}**: Opens Playwright's UI mode for interactive testing
- **codegen**: Launches Playwright's code generation tool
- **format**: Automatically formats code using Prettier
- **lint/lint:fix**: Checks/fixes code style using ESLint
- **ts:check**: Verifies TypeScript types without emitting files
- **post-testing**: Generates test reports in JUnit format

## Debugging Tests

1. Using Playwright UI Mode:
```bash
# With Maven
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.debug="--ui" \
  -De2e.test.specific=login.spec.ts

# With Node.js
yarn run start-dev --ui login.spec.ts
```

2. Using Playwright Debug Inspector:
```bash
# With Maven
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.debug="--debug" \
  -De2e.test.specific=login.spec.ts

# With Node.js
yarn run start-dev --debug login.spec.ts
```

## Writing Tests

### Using POM Pattern

To create reusable tests, we use the Page Object Model (POM) pattern. This pattern allows us to create tests that can be reused across different projects and makes our test suite more maintainable.

The POM pattern provides several benefits:
- Reduces code duplication
- Improves test maintenance
- Enhances test readability
- Separates test logic from page interactions

### Project Structure
```
src/
├── pages/      # Page Object Models
├── locators/   # Element selectors
├── utils/      # Helper functions
├── data/       # Test data
└── models/     # TypeScript interfaces
```

### TypeScript Configuration
The project includes path aliases for better import management:
```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@pages": ["./src/pages/index"],
      "@locators/*": ["./src/locators/*"],
      "@utils/*": ["./src/utils/*"],
      "@data/*": ["./src/data/*"],
      "@models/*": ["./src/models/*"]
    }
  }
}
```

### Example Test
```typescript
import { LoginPage } from '@pages';

test('should login', async () => {
  const loginPage = new LoginPage();
  await loginPage.login();
  // Add assertions here
});
```

## Troubleshooting

### Common Issues

1. **Docker containers not starting**
   - Ensure no conflicting ports
   - Check Docker daemon is running
   - Verify sufficient system resources

2. **Tests failing to connect to dotCMS**
   - Verify dotCMS is running on correct port
   - Check network connectivity
   - Ensure correct environment variables

3. **Playwright installation issues**
   - Run `yarn playwright install-deps` again
   - Check system requirements
   - Verify Node.js version compatibility

### Getting Help

- Check the [dotCMS documentation](https://dotcms.com/docs/)
- Review Playwright's [official documentation](https://playwright.dev/)
- Submit issues to the project's issue tracker

## Notes

- Test reports are automatically generated after test execution
- Non-CI environments will open HTML reports in the browser
- Tests run against the latest locally built dotCMS image when using Maven
