import { test, expect } from "@playwright/test";
import {
  LoginPage,
  PagesListPage,
} from "@pages";

test.beforeEach(async ({ page }) => {
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;
  const loginPage = new LoginPage(page);
  await loginPage.loginAndOpenSideMenu(username, password);
});

test("should display the pages list", async ({ page }) => {
  const pagesListPage = new PagesListPage(page);
  await pagesListPage.navigateTo();
  const pageListItems = pagesListPage.getPageListItems();
  await expect(pageListItems).not.toHaveCount(0);
});
