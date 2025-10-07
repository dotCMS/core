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
  const listingContentTypesPage = new ListingContentTypesPage(page, request);
  const contentTypeFormPage = new ContentTypeFormPage(page);

  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  const loginPage = new LoginPage(page);
  await loginPage.loginAndOpenSideMenu(username, password);

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

test("should save a text field", async ({ page }) => {
  const locatorField = page.getByTestId("textField");

  await expect(locatorField).toBeVisible();

  const newEditContentFormPage = new NewEditContentFormPage(page);

  const textFieldValue = faker.lorem.word();

  await newEditContentFormPage.fillTextField(textFieldValue);
  await newEditContentFormPage.save();

  const listingContentPage = new ListingContentPage(page);
  await listingContentPage.goTo(contentTypeName);
  await listingContentPage.clickFirstContentRow();

  await expect(locatorField).toHaveValue(textFieldValue);
});
