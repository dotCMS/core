import { test, expect } from "@playwright/test";
import { BreadcrumbComponent } from "@components/breadcrumb.component";
import { LoginPage } from "@pages";
import {
  archiveTemplate,
  createTemplate,
  deleteTemplate,
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
  // Wait for page to be fully loaded
  await page.waitForLoadState("load");

  const breadcrumb = new BreadcrumbComponent(page);
  const title = breadcrumb.getTitle();
  // Wait for title to appear, which indicates page is ready
  await title.waitFor({ state: "visible" });
  await expect(title).toHaveText("Welcome");
});

test("should display correctly on the Site Browser page", async ({ page }) => {
  await page.goto("/dotAdmin/#/c/site-browser");
  await page.waitForLoadState("load");

  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await breadcrumbText.waitFor({ state: "visible" });
  await expect(breadcrumbText).toContainText("Home");
  await expect(breadcrumbText).toContainText("Site");

  const title = breadcrumb.getTitle();
  await title.waitFor({ state: "visible" });
  await expect(title).toHaveText("Browser");
});

test("should display correctly on the Pages page", async ({ page }) => {
  await page.goto("/dotAdmin/#/pages");
  await page.waitForLoadState("load");

  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await breadcrumbText.waitFor({ state: "visible" });
  await expect(breadcrumbText).toContainText("Home");
  await expect(breadcrumbText).toContainText("Site");

  const title = breadcrumb.getTitle();
  await title.waitFor({ state: "visible" });
  await expect(title).toHaveText("Pages");
});

test("should display correctly on the Containers page", async ({ page }) => {
  await page.goto("/dotAdmin/#/containers");
  await page.waitForLoadState("domcontentloaded");

  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await breadcrumbText.waitFor({ state: "visible" });
  await expect(breadcrumbText).toContainText("Home");
  await expect(breadcrumbText).toContainText("Site");

  const title = breadcrumb.getTitle();
  await title.waitFor({ state: "visible" });
  await expect(title).toHaveText("Containers");
});

test("should display correctly on the Content Types page", async ({ page }) => {
  await page.goto("/dotAdmin/#/content-types-angular");
  await page.waitForLoadState("load");

  const breadcrumb = new BreadcrumbComponent(page);
  const breadcrumbText = breadcrumb.getBreadcrumb();
  await breadcrumbText.waitFor({ state: "visible" });
  await expect(breadcrumbText).toContainText("Home");
  await expect(breadcrumbText).toContainText("Schema");

  const title = breadcrumb.getTitle();
  await title.waitFor({ state: "visible" });
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

  test("should display correctly on the Template page", async ({ page }) => {
    await page.goto(`/dotAdmin/#/templates/edit/${template.identifier}`);
    await page.waitForLoadState("load");

    const breadcrumb = new BreadcrumbComponent(page);
    const title = breadcrumb.getTitle();

    // Wait for title to change to template title (indicates page loaded)
    await expect(title).toHaveText(template.title, { timeout: 15000 });

    const breadcrumbText = breadcrumb.getBreadcrumb();
    await expect(breadcrumbText).toContainText("Home", { timeout: 10000 });
    await expect(breadcrumbText).toContainText("Site");
    await expect(breadcrumbText).toContainText("Templates");
  });

  test.afterEach(async ({ request }) => {
    if (template) {
      await archiveTemplate(request, [template.identifier]);
      await deleteTemplate(request, [template.identifier]);
    }
  });
});
