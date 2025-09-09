import { test, expect } from "@playwright/test";
import { BreadcrumbComponent } from "@components/breadcrumb.component";
import { LoginPage } from "@pages";
import {
  archiveTemplate,
  createTemplate,
  deleteTemplate,
  getTemplate,
} from "@requests/templates";
import { Template } from "@models/template.model";

test.beforeEach("Login", async ({ page }) => {
  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  const loginPage = new LoginPage(page);
  await loginPage.loginAndOpenSideMenu(username, password);
});

test("should display correctly on the Starter page", async ({ page }) => {
  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await expect(breadcrumbText).toHaveText("Getting Started");

  const title = breadcrumb.getTitle();
  await expect(title).toHaveText("Welcome");
});

test("should display correctly on the Site Browser page", async ({ page }) => {
  await page.goto("/dotAdmin/#/c/site-browser");
  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await expect(breadcrumbText).toHaveText("Site Manager");

  const title = breadcrumb.getTitle();
  await expect(title).toHaveText("Browser");
});

test("should display correctly on the Pages page", async ({ page }) => {
  await page.goto("/dotAdmin/#/pages");
  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await expect(breadcrumbText).toHaveText("Site Manager");

  const title = breadcrumb.getTitle();
  await expect(title).toHaveText("Pages");
});

test("should display correctly on the Containers page", async ({ page }) => {
  await page.goto("/dotAdmin/#/containers");
  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await expect(breadcrumbText).toHaveText("Site Manager");

  const title = breadcrumb.getTitle();
  await expect(title).toHaveText("Containers");
});

test("should display correctly on the Content Types page", async ({ page }) => {
  await page.goto("/dotAdmin/#/content-types-angular");
  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await expect(breadcrumbText).toHaveText("Schema");

  const title = breadcrumb.getTitle();
  await expect(title).toHaveText("Content Types");
});

test.describe("Template page", () => {
  let template: Template | null = null;

  test.beforeEach(async ({ request }) => {
    template = await createTemplate(request, {
      friendlyName: "Test Template",
      image: "test-image",
      theme: "test-theme",
      title: "Test Template",
    });
  });

  test("should display correctly on the Template page", async ({ page, request }) => {
    // Debug: Verify template was created successfully
    expect(template).not.toBeNull();
    expect(template?.identifier).toBeDefined();
    console.log(`Template created with ID: ${template?.identifier}`);

    // 1. Verify template accessibility via API before navigation with retry logic
    let verificationTemplate;
    let retryCount = 0;
    const maxRetries = 5;
    const retryDelay = 1000; // 1 second between retries

    while (retryCount < maxRetries) {
      try {
        verificationTemplate = await getTemplate(request, template.identifier);
        console.log(`Template verification successful on attempt ${retryCount + 1} - Title: ${verificationTemplate.title}, Live: ${verificationTemplate.live}`);
        expect(verificationTemplate).toBeTruthy();
        expect(verificationTemplate.identifier).toBe(template.identifier);
        break; // Success, exit retry loop
      } catch (error) {
        retryCount++;
        console.warn(`Template verification failed (attempt ${retryCount}/${maxRetries}): ${error.message}`);

        if (retryCount >= maxRetries) {
          console.error('Template verification failed after all retries:', error);
          throw new Error(`Template ${template.identifier} is not accessible via API after ${maxRetries} attempts: ${error.message}`);
        }

        // Wait before retrying
        await new Promise(resolve => setTimeout(resolve, retryDelay));
      }
    }

    // 2. Navigate to the template edit page with comprehensive error handling
    const targetUrl = `/dotAdmin/#/templates/edit/${template.identifier}`;
    console.log(`Navigating to: ${targetUrl}`);

    await page.goto(targetUrl);

    // 3. Wait for navigation with detailed error reporting
    try {
      await page.waitForURL('**/templates/edit/**', { timeout: 15000 });
      console.log(`Successfully navigated to template edit page: ${page.url()}`);
    } catch (navigationError) {
      const currentUrl = page.url();
      console.error(`Navigation failed - Expected: **/templates/edit/**, Current: ${currentUrl}`);

      // Check if we're redirected back to templates list (resolver failure)
      if (currentUrl.includes('/templates') && !currentUrl.includes('/edit/')) {
        console.error('Redirected back to templates list - likely resolver failure (template not found or permission denied)');

        // Check network tab for any API errors
        const responses = await page.evaluate(() => {
          return performance.getEntries()
            .filter(entry => entry.name.includes('/api/v1/templates/'))
            .map(entry => ({ url: entry.name, duration: entry.duration }));
        });
        console.log('Template API calls:', responses);
      }

      // Get current breadcrumb for debugging
      try {
        const currentBreadcrumb = await page.getByTestId('breadcrumb-crumbs').textContent();
        console.error(`Current breadcrumb: "${currentBreadcrumb}"`);
      } catch (breadcrumbError) {
        console.error('Could not read breadcrumb:', breadcrumbError.message);
      }

      throw new Error(`Failed to navigate to template edit page. Current URL: ${currentUrl}, Expected pattern: **/templates/edit/**`);
    }

    await page.waitForLoadState('networkidle');

    // 4. Wait for the template page to be fully loaded with better error handling
    try {
      await page.waitForSelector('[data-testid="breadcrumb-crumbs"]', { timeout: 15000 });
      console.log('Breadcrumb element found successfully');
    } catch (selectorError) {
      console.error('Breadcrumb selector not found - page may not be fully loaded');

      // Check if the page has any breadcrumb-related elements
      const breadcrumbElements = await page.locator('[data-testid*="breadcrumb"]').all();
      console.log(`Found ${breadcrumbElements.length} breadcrumb-related elements`);

      for (let i = 0; i < breadcrumbElements.length; i++) {
        const element = breadcrumbElements[i];
        const testId = await element.getAttribute('data-testid');
        const text = await element.textContent().catch(() => 'N/A');
        console.log(`Breadcrumb element ${i}: testid="${testId}", text="${text}"`);
      }

      const pageContent = await page.textContent('body');
      console.error('Page content preview:', pageContent.substring(0, 1000));
      throw selectorError;
    }

    // 5. Verify the breadcrumb and title
    const breadcrumb = new BreadcrumbComponent(page);
    const breadcrumbText = breadcrumb.getBreadcrumb();

    try {
      await expect(breadcrumbText).toHaveText("Site ManagerTemplates");
    } catch (breadcrumbError) {
      try {
        const actualText = await breadcrumbText.textContent({ timeout: 5000 });
        console.error(`Breadcrumb mismatch - Expected: "Site ManagerTemplates", Actual: "${actualText}"`);
        throw new Error(`Breadcrumb text mismatch. Expected: "Site ManagerTemplates", Actual: "${actualText}"`);
      } catch (textError) {
        console.error('Could not read breadcrumb text:', textError.message);
        // Let's capture the entire page content for debugging
        const pageContent = await page.content();
        console.log('Full page content preview:', pageContent.substring(0, 1000));
        throw new Error('Breadcrumb element not found or not accessible');
      }
    }

    const title = breadcrumb.getTitle();

    try {
      await expect(title).toHaveText(template.title);
    } catch (titleError) {
      const actualTitle = await title.textContent();
      console.error(`Title mismatch - Expected: "${template.title}", Actual: "${actualTitle}"`);
      throw titleError;
    }

    console.log('Template page breadcrumb test completed successfully');
  });

  test.afterEach(async ({ request }) => {
    if (template) {
      await archiveTemplate(request, [template.identifier]);
      await deleteTemplate(request, [template.identifier]);
    }
  });
});
