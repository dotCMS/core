import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import { ListingContentTypesPage } from "@pages/listingContentTypes.pages";
import { ContentTypeFormPage } from "@pages/contentTypeForm.page";
import { NewEditContentFormPage } from "@pages/newEditContentForm.page";
import { ListingContentPage } from "@pages/listngContent.page";
import { dotCMSUtils } from "@utils/dotCMSUtils";
import { createDefaultContentType } from "@data/defaultContentType";

const contentTypeName = faker.lorem.word().toLocaleLowerCase();

test.beforeEach("Navigate to content types", async ({ page, request }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page, request);
  const contentTypeFormPage = new ContentTypeFormPage(page);

  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  const cmsUtils = new dotCMSUtils();
  await cmsUtils.login(page, username, password);

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

test.skip("should save a text field", async ({ page }) => {
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
