import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import {
  ListingContentTypesPage,
  ContentTypeFormPage,
  NewEditContentFormPage,
  ListingContentPage,
  LoginPage,
} from "@pages";
import { createDefaultContentType } from "@data/defaultContentType";

const contentTypeName = faker.lorem.word().toLocaleLowerCase();

test.beforeEach("Navigate to content types", async ({ page, request }) => {
  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;
  const loginPage = new LoginPage(page);

  await loginPage.login(username, password);

  const listingContentTypesPage = new ListingContentTypesPage(page, request);
  const contentTypeFormPage = new ContentTypeFormPage(page);

  await listingContentTypesPage.toggleNewContentEditor(true);
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.addNewContentType(contentTypeName);
  await contentTypeFormPage.createNewContentType(createDefaultContentType());
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.goToAddNewContentType(contentTypeName);
});

test.afterEach(async ({ page, request }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page, request);
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.deleteContentType(contentTypeName);
  await listingContentTypesPage.toggleNewContentEditor(false);
});

test.skip("should save a site or folder field", async ({ page }) => {
  const locatorFieldLocator = page.getByTestId("field-siteOrFolderField");
  await expect(locatorFieldLocator).toBeVisible();

  const newEditContentFormPage = new NewEditContentFormPage(page);

  const selectedFolder = await newEditContentFormPage.selectSiteOrFolderField();
  await newEditContentFormPage.save();

  const listingContentPage = new ListingContentPage(page);
  await listingContentPage.goTo(contentTypeName);
  await listingContentPage.clickFirstContentRow();

  await expect(locatorFieldLocator).toHaveText(`//${selectedFolder}`);
});
