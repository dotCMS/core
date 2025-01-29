import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import {
  ListingContentTypesPage,
  ContentTypeFormPage,
  NewEditContentFormPage,
  ListingContentPage,
  LoginPage,
  SideMenuPage,
} from "@pages/index";

const contentTypeName = faker.lorem.word().toLocaleLowerCase();

test.beforeEach("Navigate to content types", async ({ page, request }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page, request);
  const contentTypeFormPage = new ContentTypeFormPage(page);

  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  const loginPage = new LoginPage(page);
  await loginPage.login(username, password);

  const sideMenuPage = new SideMenuPage(page);
  await sideMenuPage.navigate("Schema", "Content Types");

  await listingContentTypesPage.toggleNewContentEditor(true);
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.addNewContentType(contentTypeName);
  await contentTypeFormPage.fillNewContentType();
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
  const listingContentPage = new ListingContentPage(page);

  const textFieldValue = faker.lorem.word();

  await newEditContentFormPage.fillTextField(textFieldValue);
  await newEditContentFormPage.save();
  await newEditContentFormPage.goToBack();
  await listingContentPage.clickFirstContentRow();

  await expect(locatorField).toHaveValue(textFieldValue);
});
