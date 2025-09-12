# dotCMS E2E Testing Commands

## Available Commands

### 🚀 **Development Mode (with Angular dev server)**

#### Normal Mode (Browser visible)

```bash
yarn e2e:dev
```

-   ✅ Runs Angular dev server automatically on port 4200
-   ✅ Browser window visible (headed mode)
-   ✅ **Opens HTML report automatically** when tests finish
-   ✅ Uses proxy to dotCMS backend on port 8080

#### Headless Mode (No browser window)

```bash
yarn e2e:dev:headless
```

-   ✅ Runs Angular dev server automatically on port 4200
-   ✅ Browser runs in headless mode (faster)
-   ✅ **Opens HTML report automatically** when tests finish
-   ✅ Uses proxy to dotCMS backend on port 8080

### 🏗️ **CI Mode (direct dotCMS connection)**

```bash
yarn e2e:ci
```

-   ✅ Connects directly to dotCMS on port 8080
-   ✅ Headless mode (no browser window)
-   ✅ Report generated but not opened automatically
-   ✅ Optimized for CI/CD environments

### 🎨 **Interactive UI Mode**

```bash
yarn e2e:ui
```

-   ✅ Opens Playwright's interactive UI
-   ✅ Perfect for debugging and test development
-   ✅ Step-by-step test execution

### 🎯 **Running Specific Tests**

```bash
# Run specific test by name
yarn e2e:dev --grep "Login"

# Run specific test file
yarn e2e:dev src/tests/login/login.spec.ts

# Run with different reporter
yarn e2e:dev --reporter=list
```

## Environment Configuration

### Development Environment

-   **Base URL**: `http://localhost:4200` (Angular dev server)
-   **Backend**: Proxied to dotCMS on `http://localhost:8080`
-   **Report**: Opens automatically in browser
-   **WebServer**: Starts Angular automatically

### CI Environment

-   **Base URL**: `http://localhost:8080` (Direct dotCMS)
-   **Backend**: Direct connection to dotCMS
-   **Report**: Generated but not opened
-   **WebServer**: None (assumes dotCMS is already running)

## Reports and Debugging

### HTML Report

-   **Location**: `dist/.playwright/apps/dotcms-ui-e2e/`
-   **Auto-open**: Only in dev mode (`e2e:dev` and `e2e:dev:headless`)
-   **Manual open**: `npx playwright show-report`

### Screenshots and Videos

-   **Screenshots**: Taken on test failure
-   **Videos**: Recorded on test failure
-   **Traces**: Captured on retry
-   **Location**: `dist/.playwright/apps/dotcms-ui-e2e/test-output/`

## Best Practices

### For Development

1. Use `yarn e2e:dev` for normal development
2. Use `yarn e2e:dev:headless` for faster feedback
3. Use `yarn e2e:ui` for debugging specific tests
4. Use `--grep` to run specific tests during development

### For CI/CD

1. Use `yarn e2e:ci` in CI environments
2. Ensure dotCMS is running on port 8080
3. Reports are generated but not opened automatically

### Test Writing

1. Always follow POM (Page Object Model) patterns
2. Use `data-testid` selectors with `page.getByTestId()`
3. Never interact directly with DOM in tests
4. Centralize test data in separate files

## Environment Variables

-   `CURRENT_ENV`: `dev` | `ci` (default: `dev`)
-   `HEADLESS`: `true` | `false` (overrides environment default)
-   `E2E_BASE_URL`: Custom base URL (overrides environment default)
-   `E2E_REUSE_EXISTING_SERVER`: `true` | `false` (default: `true`)

## Examples

```bash
# Quick test during development
yarn e2e:dev --grep "login"

# Full test suite in headless mode
yarn e2e:dev:headless

# Debug specific failing test
yarn e2e:ui src/tests/login/login.spec.ts

# CI mode (for GitHub Actions, etc.)
yarn e2e:ci
```
