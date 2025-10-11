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
    // Verify template was created successfully
    expect(template).not.toBeNull();
    expect(template?.identifier).toBeDefined();

    // Verify template accessibility via API with retry logic
    let verificationTemplate;
    let retryCount = 0;
    const maxRetries = 3;
    const retryDelay = 1000;

    while (retryCount < maxRetries) {
      try {
        verificationTemplate = await getTemplate(request, template.identifier);
        expect(verificationTemplate).toBeTruthy();
        expect(verificationTemplate.identifier).toBe(template.identifier);
        break;
      } catch (error) {
        retryCount++;
        if (retryCount >= maxRetries) {
          throw new Error(`Template ${template.identifier} is not accessible via API after ${maxRetries} attempts`);
        }
        await new Promise(resolve => setTimeout(resolve, retryDelay));
      }
    }

    // Navigate to the template edit page
    const targetUrl = `/dotAdmin/#/templates/edit/${template.identifier}`;
    await page.goto(targetUrl);

    // Wait for navigation
    await page.waitForURL('**/templates/edit/**', { timeout: 10000 });
    await page.waitForLoadState('networkidle');

    // Wait for the breadcrumb to be available
    await page.waitForSelector('[data-testid="breadcrumb-crumbs"]', { timeout: 10000 });

    // Verify the breadcrumb and title
    const breadcrumb = new BreadcrumbComponent(page);
    const breadcrumbText = breadcrumb.getBreadcrumb();
    await expect(breadcrumbText).toHaveText("Site ManagerTemplates");

    const title = breadcrumb.getTitle();
    // Wait for the title to be populated (it may be empty initially while template data loads)
    await expect(title).not.toHaveText("", { timeout: 10000 });
    await expect(title).toHaveText(template.title);
  });

  test.afterEach(async ({ request }) => {
    if (template) {
      await archiveTemplate(request, [template.identifier]);
      await deleteTemplate(request, [template.identifier]);
    }
  });
});
