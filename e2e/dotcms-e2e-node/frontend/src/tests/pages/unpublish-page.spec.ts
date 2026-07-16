import { test, expect } from "@playwright/test";
import { LoginPage, PagesListPage } from "@pages";
import { createPage, Page, actionsPageWorkflow } from "@requests/pages";
import { faker } from "@faker-js/faker";

let pageContentlet: Page | null = null;

test.beforeEach(async ({ page, request }) => {
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;
  const loginPage = new LoginPage(page);
  await loginPage.loginAndOpenSideMenu(username, password);

  const randomTitle = faker.lorem.words(3);
  const randomUrl = randomTitle.split(" ").join("-");

  pageContentlet = await createPage(request, {
    friendlyName: randomTitle,
    title: randomTitle,
    url: randomUrl,
    hostFolder: "default",
    template: "SYSTEM_TEMPLATE",
    contentType: "htmlpageasset",
    cachettl: 0,
  });
});

test.afterEach(async ({ request }) => {
  if (pageContentlet) {
    await actionsPageWorkflow(request, pageContentlet.inode, [
      "Archive",
      "Destroy",
    ]);
  }
});

test("should unpublish the page", async ({ page }) => {
  const pagesListPage = new PagesListPage(page);
  await pagesListPage.navigateTo();

  const rowLocator = pagesListPage.getRowByTitle(pageContentlet.title);
  await expect(rowLocator).toBeVisible();

  await pagesListPage.doActionOnPage(rowLocator, "Unpublish");
  const statusIcon = pagesListPage.getStatusIcon(rowLocator);
  await expect(statusIcon).toHaveAttribute("aria-label", "Draft");
});
