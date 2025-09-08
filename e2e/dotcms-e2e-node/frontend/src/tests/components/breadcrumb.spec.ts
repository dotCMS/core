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

  test.skip("should display correctly on the Template page", async ({ page }) => {
    await page.goto(`/dotAdmin/#/templates/edit/${template.identifier}`);
    const breadcrumb = new BreadcrumbComponent(page);

    const breadcrumbText = breadcrumb.getBreadcrumb();
    await expect(breadcrumbText).toHaveText("Site ManagerTemplates");

    const title = breadcrumb.getTitle();
    await expect(title).toHaveText(template.title);
  });

  test.afterEach(async ({ request }) => {
    if (template) {
      await archiveTemplate(request, [template.identifier]);
      await deleteTemplate(request, [template.identifier]);
    }
  });
});
