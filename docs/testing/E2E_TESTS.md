# End-to-End (E2E) Tests

## Overview

dotCMS E2E tests validate complete user workflows across the entire application stack. The project includes both modern Playwright-based tests and legacy Selenium tests, with Playwright being the preferred approach for new test development.

## Test Structure

### Location & Architecture
- **Modern E2E**: `e2e/dotcms-e2e-node/` (Playwright + TypeScript)
- **Legacy E2E**: `e2e/dotcms-e2e-java/` (Selenium + Java)
- **Status**: Playwright is preferred, legacy tests being gradually replaced
- **Integration**: Partially integrated into CI/CD pipeline

### Project Structure
```
e2e/
├── dotcms-e2e-node/              # Modern Playwright tests
│   ├── src/
│   │   ├── tests/                # Test files
│   │   ├── pages/                # Page Object Model
│   │   ├── fixtures/             # Test data
│   │   └── utils/                # Test utilities
│   ├── playwright.config.ts      # Playwright configuration
│   ├── package.json              # Node.js dependencies
│   └── pom.xml                   # Maven integration
├── dotcms-e2e-java/              # Legacy Selenium tests
│   ├── src/test/java/            # Java test files
│   ├── src/test/resources/       # Test resources
│   └── pom.xml                   # Maven configuration
└── docker/                       # Docker test environment
```

## Playwright E2E Tests (Modern)

### Configuration
```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    testDir: './src/tests',
    outputDir: './test-results',
    timeout: 30000,
    expect: {
        timeout: 5000
    },
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: [
        ['html'],
        ['junit', { outputFile: 'junit-results.xml' }],
        ['json', { outputFile: 'test-results.json' }]
    ],
    use: {
        baseURL: process.env.BASE_URL || 'http://localhost:8080',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure'
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        },
        {
            name: 'firefox',
            use: { ...devices['Desktop Firefox'] }
        },
        {
            name: 'webkit',
            use: { ...devices['Desktop Safari'] }
        },
        {
            name: 'mobile-chrome',
            use: { ...devices['Pixel 5'] }
        }
    ]
});
```

### Page Object Model
```typescript
// src/pages/LoginPage.ts
import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
    private page: Page;
    private usernameInput: Locator;
    private passwordInput: Locator;
    private loginButton: Locator;
    private errorMessage: Locator;

    constructor(page: Page) {
        this.page = page;
        this.usernameInput = page.locator('[data-testid="username-input"]');
        this.passwordInput = page.locator('[data-testid="password-input"]');
        this.loginButton = page.locator('[data-testid="login-button"]');
        this.errorMessage = page.locator('[data-testid="error-message"]');
    }

    async goto() {
        await this.page.goto('/login');
    }

    async login(username: string, password: string) {
        await this.usernameInput.fill(username);
        await this.passwordInput.fill(password);
        await this.loginButton.click();
    }

    async expectLoginSuccess() {
        await expect(this.page).toHaveURL(/.*dashboard/);
    }

    async expectLoginError(errorText: string) {
        await expect(this.errorMessage).toBeVisible();
        await expect(this.errorMessage).toContainText(errorText);
    }

    async isLoginFormVisible() {
        return await this.loginButton.isVisible();
    }
}
```

### Test Examples

#### Basic Login Test
```typescript
// src/tests/auth/login.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';

test.describe('Login Functionality', () => {
    test('should login with valid credentials', async ({ page }) => {
        const loginPage = new LoginPage(page);
        const dashboardPage = new DashboardPage(page);

        await loginPage.goto();
        await loginPage.login('admin@dotcms.com', 'admin');
        await loginPage.expectLoginSuccess();
        await dashboardPage.expectDashboardVisible();
    });

    test('should show error with invalid credentials', async ({ page }) => {
        const loginPage = new LoginPage(page);

        await loginPage.goto();
        await loginPage.login('invalid@email.com', 'wrongpassword');
        await loginPage.expectLoginError('Invalid credentials');
    });

    test('should redirect to login when accessing protected route', async ({ page }) => {
        await page.goto('/dashboard');
        await expect(page).toHaveURL(/.*login/);
    });
});
```

#### Content Management Test
```typescript
// src/tests/content/content-management.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { ContentPage } from '../pages/ContentPage';
import { ContentEditor } from '../pages/ContentEditor';

test.describe('Content Management', () => {
    test.beforeEach(async ({ page }) => {
        const loginPage = new LoginPage(page);
        await loginPage.goto();
        await loginPage.login('admin@dotcms.com', 'admin');
        await loginPage.expectLoginSuccess();
    });

    test('should create new content', async ({ page }) => {
        const contentPage = new ContentPage(page);
        const contentEditor = new ContentEditor(page);

        await contentPage.goto();
        await contentPage.clickCreateContent();
        await contentPage.selectContentType('webPageContent');

        await contentEditor.fillTitle('Test Page Title');
        await contentEditor.fillBody('This is test content body');
        await contentEditor.save();

        await expect(contentEditor.getSuccessMessage()).toBeVisible();
        await expect(contentEditor.getSuccessMessage()).toContainText('Content saved successfully');
    });

    test('should edit existing content', async ({ page }) => {
        const contentPage = new ContentPage(page);
        const contentEditor = new ContentEditor(page);

        await contentPage.goto();
        await contentPage.searchContent('Test Page Title');
        await contentPage.clickEditContent();

        await contentEditor.fillTitle('Updated Test Page Title');
        await contentEditor.save();

        await expect(contentEditor.getSuccessMessage()).toBeVisible();
        await contentPage.expectContentInList('Updated Test Page Title');
    });

    test('should delete content', async ({ page }) => {
        const contentPage = new ContentPage(page);

        await contentPage.goto();
        await contentPage.searchContent('Updated Test Page Title');
        await contentPage.clickDeleteContent();
        await contentPage.confirmDelete();

        await expect(contentPage.getEmptyStateMessage()).toBeVisible();
    });
});
```

#### Workflow Integration Test
```typescript
// src/tests/workflow/workflow.spec.ts
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { WorkflowPage } from '../pages/WorkflowPage';
import { ContentPage } from '../pages/ContentPage';

test.describe('Workflow Integration', () => {
    test.beforeEach(async ({ page }) => {
        const loginPage = new LoginPage(page);
        await loginPage.goto();
        await loginPage.login('admin@dotcms.com', 'admin');
        await loginPage.expectLoginSuccess();
    });

    test('should move content through workflow states', async ({ page }) => {
        const contentPage = new ContentPage(page);
        const workflowPage = new WorkflowPage(page);

        // Create content in draft state
        await contentPage.goto();
        await contentPage.createContent('Test Workflow Content');
        await expect(contentPage.getContentStatus()).toContainText('Draft');

        // Move to review state
        await workflowPage.selectWorkflowAction('Send for Review');
        await workflowPage.confirmAction();
        await expect(contentPage.getContentStatus()).toContainText('Review');

        // Approve and publish
        await workflowPage.selectWorkflowAction('Approve');
        await workflowPage.confirmAction();
        await expect(contentPage.getContentStatus()).toContainText('Published');
    });

    test('should handle workflow rejection', async ({ page }) => {
        const contentPage = new ContentPage(page);
        const workflowPage = new WorkflowPage(page);

        await contentPage.goto();
        await contentPage.createContent('Test Rejection Content');
        await workflowPage.selectWorkflowAction('Send for Review');
        await workflowPage.confirmAction();

        // Reject content
        await workflowPage.selectWorkflowAction('Reject');
        await workflowPage.addComment('Content needs improvement');
        await workflowPage.confirmAction();

        await expect(contentPage.getContentStatus()).toContainText('Draft');
        await expect(workflowPage.getWorkflowComment()).toContainText('Content needs improvement');
    });
});
```

### Test Utilities
```typescript
// src/utils/test-helpers.ts
import { Page } from '@playwright/test';

export class TestHelpers {
    static async waitForNetworkIdle(page: Page) {
        await page.waitForLoadState('networkidle');
    }

    static async clearLocalStorage(page: Page) {
        await page.evaluate(() => localStorage.clear());
    }

    static async mockApiResponse(page: Page, url: string, response: any) {
        await page.route(url, route => {
            route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(response)
            });
        });
    }

    static generateUniqueId(): string {
        return `test_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    static async takeScreenshot(page: Page, name: string) {
        await page.screenshot({ 
            path: `./screenshots/${name}.png`,
            fullPage: true
        });
    }
}
```

### Running Playwright Tests
```bash
# Install dependencies
cd e2e/dotcms-e2e-node && npm install

# Run all tests
npx playwright test

# Run specific test file
npx playwright test src/tests/auth/login.spec.ts

# Run tests in headed mode
npx playwright test --headed

# Run tests with specific browser
npx playwright test --project=chromium

# Run tests with debug mode
npx playwright test --debug

# Generate test report
npx playwright show-report

# Run tests with Maven
./mvnw verify -De2e.test.skip=false -pl :dotcms-e2e-node
```

## Legacy Selenium Tests (Java)

### Structure
```java
// Base test class
public abstract class BaseE2ETest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    
    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:8080");
    }
    
    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

// Page Object example
public class LoginPageSelenium {
    private WebDriver driver;
    private WebDriverWait wait;
    
    @FindBy(id = "username")
    private WebElement usernameInput;
    
    @FindBy(id = "password")
    private WebElement passwordInput;
    
    @FindBy(css = "[data-testid='login-button']")
    private WebElement loginButton;
    
    public LoginPageSelenium(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }
    
    public void login(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        loginButton.click();
    }
    
    public void waitForDashboard() {
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }
}
```

### Test Example
```java
public class LoginE2ETest extends BaseE2ETest {
    
    @Test
    public void testSuccessfulLogin() {
        LoginPageSelenium loginPage = new LoginPageSelenium(driver);
        
        loginPage.login("admin@dotcms.com", "admin");
        loginPage.waitForDashboard();
        
        assertTrue(driver.getCurrentUrl().contains("/dashboard"));
    }
}
```

## CI/CD Integration

### GitHub Actions Configuration
**Workflow**: E2E tests run conditionally in CI/CD pipeline

**Change Detection**: Tests triggered by:
```yaml
frontend: &frontend
  - 'core-web/**'
  - 'e2e/**'
  - 'package.json'
```

**Execution**:
```yaml
- name: Install Playwright
  run: |
    cd e2e/dotcms-e2e-node
    npm ci
    npx playwright install

- name: Run E2E Tests
  run: |
    cd e2e/dotcms-e2e-node
    npx playwright test --project=chromium
  env:
    BASE_URL: http://localhost:8080
    CI: true

- name: Upload Test Results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: playwright-report
    path: e2e/dotcms-e2e-node/playwright-report/
```

### Docker Environment
```yaml
# docker-compose.e2e.yml
version: '3.8'
services:
  dotcms:
    image: dotcms/dotcms:latest
    ports:
      - "8080:8080"
    environment:
      - DB_BASE_URL=jdbc:postgresql://postgres:5432/dotcms
    depends_on:
      - postgres
  
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=dotcms
      - POSTGRES_USER=dotcms
      - POSTGRES_PASSWORD=dotcms
```

## Debugging E2E Test Failures

### Local Debugging

#### 1. Playwright Debug Mode
```bash
# Run in debug mode
npx playwright test --debug

# Run with headed browser
npx playwright test --headed --slowMo=1000

# Run specific test with debug
npx playwright test src/tests/auth/login.spec.ts --debug
```

#### 2. Visual Debugging
```typescript
// Add debug steps in test
test('debug test', async ({ page }) => {
    await page.goto('/login');
    
    // Pause for manual inspection
    await page.pause();
    
    // Take screenshot
    await page.screenshot({ path: 'debug.png' });
    
    // Console log
    console.log('Current URL:', page.url());
});
```

#### 3. Test Trace Analysis
```bash
# Enable trace recording
npx playwright test --trace=on

# View trace
npx playwright show-trace trace.zip
```

### GitHub Actions Debugging

#### 1. Enable Debug Output
```yaml
- name: Run E2E Tests with Debug
  run: |
    cd e2e/dotcms-e2e-node
    DEBUG=pw:* npx playwright test
  env:
    PLAYWRIGHT_BROWSER_WS_ENDPOINT: ws://localhost:3000
```

#### 2. Upload Debug Artifacts
```yaml
- name: Upload Debug Artifacts
  uses: actions/upload-artifact@v4
  if: failure()
  with:
    name: e2e-debug-artifacts
    path: |
      e2e/dotcms-e2e-node/test-results/
      e2e/dotcms-e2e-node/playwright-report/
      e2e/dotcms-e2e-node/screenshots/
```

#### 3. Common Failure Patterns

**Timing Issues**:
```typescript
// Wait for element to be ready
await expect(page.locator('[data-testid="element"]')).toBeVisible();

// Wait for network requests
await page.waitForResponse('**/api/content');

// Wait for specific condition
await page.waitForFunction(() => document.readyState === 'complete');
```

**Element Selection Issues**:
```typescript
// Use reliable selectors
const button = page.locator('[data-testid="submit-button"]');
const input = page.locator('input[name="username"]');

// Avoid dynamic selectors
const dynamicElement = page.locator('div:nth-child(3)'); // ❌ Fragile
const stableElement = page.locator('[data-testid="content-item"]'); // ✅ Stable
```

## Best Practices

### ✅ E2E Testing Standards
- **Use Page Object Model**: Maintain clear separation of concerns
- **Data-testid attributes**: Reliable element identification
- **Independent tests**: Each test should be able to run in isolation
- **Realistic user flows**: Test complete user journeys
- **Cross-browser testing**: Validate compatibility across browsers

### ✅ Test Organization
```
src/
├── tests/
│   ├── auth/                     # Authentication tests
│   ├── content/                  # Content management tests
│   ├── workflow/                 # Workflow tests
│   └── admin/                    # Admin functionality tests
├── pages/
│   ├── BasePage.ts               # Base page class
│   ├── LoginPage.ts              # Login page object
│   └── ContentPage.ts            # Content management page
├── fixtures/
│   ├── users.json                # User test data
│   └── content.json              # Content test data
└── utils/
    ├── test-helpers.ts           # Utility functions
    └── api-helpers.ts            # API interaction helpers
```

### ✅ Performance Optimization
```typescript
// Parallel test execution
test.describe.configure({ mode: 'parallel' });

// Reuse browser context
test.describe('Content Tests', () => {
    let context: BrowserContext;
    
    test.beforeAll(async ({ browser }) => {
        context = await browser.newContext();
    });
    
    test.afterAll(async () => {
        await context.close();
    });
});

// Optimize wait strategies
await page.waitForLoadState('networkidle');
await page.waitForSelector('[data-testid="content"]', { state: 'visible' });
```

## Test Data Management

### Dynamic Test Data
```typescript
// Generate unique test data
const testData = {
    title: `Test Content ${Date.now()}`,
    body: `Test body content ${Math.random().toString(36).substr(2, 9)}`,
    tags: ['test', 'automated']
};

// Use test fixtures
import { test } from '@playwright/test';
import testUsers from '../fixtures/users.json';

test('test with fixture data', async ({ page }) => {
    const adminUser = testUsers.admin;
    await loginPage.login(adminUser.email, adminUser.password);
});
```

### Test Cleanup
```typescript
// Cleanup after each test
test.afterEach(async ({ page }) => {
    // Clean up created content
    await page.goto('/admin/content');
    await page.locator('[data-testid="delete-test-content"]').click();
});

// Global cleanup
test.afterAll(async ({ page }) => {
    // Clean up test data
    await page.goto('/admin/cleanup');
    await page.locator('[data-testid="cleanup-test-data"]').click();
});
```

## Common Issues and Solutions

### 1. Flaky Tests
```typescript
// Add retry logic
test.describe.configure({ retries: 2 });

// Use proper waits
await expect(page.locator('[data-testid="element"]')).toBeVisible({ timeout: 10000 });

// Handle race conditions
await page.waitForFunction(() => document.readyState === 'complete');
```

### 2. Browser Compatibility
```typescript
// Browser-specific tests
test.describe('Chrome-specific tests', () => {
    test.skip(({ browserName }) => browserName !== 'chromium');
    
    test('chrome feature test', async ({ page }) => {
        // Chrome-specific test
    });
});
```

### 3. Test Environment Issues
```typescript
// Environment checks
test.beforeEach(async ({ page }) => {
    const response = await page.request.get('/api/health');
    expect(response.ok()).toBeTruthy();
});
```

## Migration from Selenium to Playwright

### Key Differences
- **Async/await**: Playwright uses modern async patterns
- **Auto-waiting**: Playwright automatically waits for elements
- **Better debugging**: Built-in debugging tools
- **Cross-browser**: Native support for multiple browsers

### Migration Examples
```java
// Selenium
WebElement element = driver.findElement(By.id("username"));
element.sendKeys("admin");
element.submit();

// Playwright equivalent
await page.fill('#username', 'admin');
await page.press('#username', 'Enter');
```

## Location Information
- **Playwright Tests**: `e2e/dotcms-e2e-node/src/tests/`
- **Page Objects**: `e2e/dotcms-e2e-node/src/pages/`
- **Test Reports**: `e2e/dotcms-e2e-node/playwright-report/`
- **Configuration**: `e2e/dotcms-e2e-node/playwright.config.ts`
- **Legacy Tests**: `e2e/dotcms-e2e-java/src/test/java/`