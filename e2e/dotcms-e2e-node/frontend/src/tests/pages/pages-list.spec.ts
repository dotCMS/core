import { test, expect } from "@playwright/test";
import {
  LoginPage,
  PagesListPage,
} from "@pages";
import {
  createPage,
  deletePageWorkflow,
  Page,
} from "@requests/pages";
import { faker } from "@faker-js/faker";

let pageContentlet: Page | null = null;

test.beforeEach(async ({ page, request }) => {
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;
  const loginPage = new LoginPage(page);
  await loginPage.loginAndOpenSideMenu(username, password);

  const title = faker.lorem.word();

  pageContentlet = await createPage(request, {
    friendlyName: faker.lorem.word(),
    title,
    url: title,
    hostFolder: "default",
    template: "SYSTEM_TEMPLATE",
    contentType: "htmlpageasset",
    cachettl: 0,
  });
});

test.afterEach(async ({ request }) => {
  if (pageContentlet) {
    await deletePageWorkflow(request, pageContentlet.inode);
  }
});

test("should display the pages list", async ({ page }) => {
  const pagesListPage = new PagesListPage(page);
  await pagesListPage.navigateTo();
  const rowLocator = pagesListPage.getRowByTitle(pageContentlet.title);
  await expect(rowLocator).toBeVisible();
});


