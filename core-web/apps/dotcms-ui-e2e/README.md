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

## 🗄️ **Data Management & Test Isolation**

### **CRITICAL: Always Use Empty Starter**

All E2E tests **MUST** assume they are running against a **clean, empty dotCMS instance** (using the `empty_20250714.zip` starter or newer). This ensures:

-   ✅ **Test Isolation**: Each test runs independently
-   ✅ **Deterministic Results**: No interference from existing data
-   ✅ **Reproducible Failures**: Same conditions every time

### **Create Your Own Test Data**

**❌ NEVER rely on existing data:**

```typescript
// DON'T DO THIS - Assumes data exists
test('Edit existing blog post', async ({ page }) => {
    await page.goto('/content/blogs/my-existing-blog');
    // This will fail on empty starter!
});
```

**✅ ALWAYS create the data you need:**

```typescript
// DO THIS - Create data in the test
test('Edit blog post', async ({ page, request }) => {
    // 1. Create the content type if needed
    const contentType = await createContentType(request, blogContentType);

    // 2. Create the test data
    const blog = await createContent(request, {
        contentType: contentType.id,
        title: 'Test Blog Post',
        body: 'Test content'
    });

    // 3. Now test the functionality
    await page.goto(`/content/edit/${blog.identifier}`);
    // Test continues...

    // 4. Clean up after test
    await deleteContent(request, blog.identifier);
});
```

### **Data Encapsulation Patterns**

#### **1. Use beforeEach/afterEach for Setup/Cleanup**

```typescript
test.describe('Content Management', () => {
    let testContent: Content;

    test.beforeEach(async ({ request }) => {
        // Create test data
        testContent = await createTestContent(request);
    });

    test.afterEach(async ({ request }) => {
        // Clean up test data
        await deleteContent(request, testContent.identifier);
    });

    test('should edit content', async ({ page }) => {
        // Use the created test data
        await page.goto(`/content/edit/${testContent.identifier}`);
    });
});
```

#### **2. Create Factory Functions**

```typescript
// src/data/factories.ts
export async function createTestBlog(request: APIRequestContext) {
    return await createContent(request, {
        contentType: 'Blog',
        title: `Test Blog ${Date.now()}`,
        body: 'Test blog content',
        author: 'Test Author'
    });
}

export async function createTestUser(request: APIRequestContext) {
    return await createUser(request, {
        email: `test-${Date.now()}@dotcms.com`,
        firstName: 'Test',
        lastName: 'User'
    });
}
```

#### **3. Use Unique Identifiers**

```typescript
// Always use timestamps or UUIDs for unique data
const uniqueTitle = `Test Content ${Date.now()}`;
const uniqueEmail = `user-${crypto.randomUUID()}@test.com`;
```

### **API Request Helpers**

Create reusable API helpers for data management:

```typescript
// src/requests/content.ts
export async function createContent(request: APIRequestContext, data: CreateContentData) {
    const response = await request.post('/api/v1/content', { data });
    return response.json();
}

export async function deleteContent(request: APIRequestContext, identifier: string) {
    await request.delete(`/api/v1/content/${identifier}`);
}

// src/requests/users.ts
export async function createUser(request: APIRequestContext, userData: UserData) {
    const response = await request.post('/api/v1/users', { data: userData });
    return response.json();
}
```

### **Why This Approach?**

1. **🔒 Isolation**: Tests don't interfere with each other
2. **🎯 Reliability**: Tests work consistently across environments
3. **🧹 Clean State**: Each test starts with known conditions
4. **🔄 Repeatability**: Tests can be run multiple times
5. **🚀 Parallel Execution**: Tests can run in parallel safely

### **Environment Considerations**

-   **Development**: Uses empty starter, creates data as needed
-   **CI**: Uses empty starter, creates data as needed
-   **Local Testing**: Always assume empty state, create required data

Remember: **If your test needs data to exist, your test should create that data!**

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
