import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import { ListingContentTypesPage } from "../../../pages/listingContentTypes.pages";
import { ContentTypeFormPage } from "../../../pages/contentTypeForm.page";
import { NewEditContentFormPage } from "../../../pages/newEditContentForm.page";
import { dotCMSUtils } from "../../../utils/dotCMSUtils";

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

test.describe("text field", () => {
  test("should save a text field", async ({ page }) => {
    const newEditContentFormPage = new NewEditContentFormPage(page);

    const locatorField = page.getByTestId("textField");

    await expect(locatorField).toBeVisible();

    const textFieldValue = faker.lorem.word();

    await newEditContentFormPage.fillTextField(textFieldValue);
    const contentId = await newEditContentFormPage.save();
    await newEditContentFormPage.goToContent(contentId);

    await expect(locatorField).toHaveValue(textFieldValue);
  });
});
