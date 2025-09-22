import { test, expect } from "@playwright/test";
import { LoginPage, NewEditContentFormPage } from "@pages";
import { ContentType, createFakeContentType, deleteContentType } from "@requests/contentType";
import { faker } from "@faker-js/faker";

let contentType: ContentType | null = null;

test.beforeEach(async ({ page, request }) => {
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;
  const loginPage = new LoginPage(page);
  await loginPage.loginAndOpenSideMenu(username, password);

  contentType = await createFakeContentType(request, {
    name: faker.lorem.words(3),
  });
});

test.afterEach(async ({ request }) => {
  if (contentType) {
    await deleteContentType(request, contentType.id);
  }
});

test("should save a site or folder field", async ({ page }) => {
  const newEditContentFormPage = new NewEditContentFormPage(page);
  await newEditContentFormPage.goToNew(contentType.name);

  const selectedFolder = await newEditContentFormPage.selectSiteOrFolderField();
  await newEditContentFormPage.save();

  const urlPattern = /\/content\/[a-f0-9-]+/;
  await page.waitForURL(urlPattern);
  await expect(page).toHaveURL(urlPattern);

  await expect(newEditContentFormPage.siteOrFolderFieldLocator).toHaveText(`//${selectedFolder}`);
});
