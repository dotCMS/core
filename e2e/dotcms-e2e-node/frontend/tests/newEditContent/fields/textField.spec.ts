import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import { ListingContentTypesPage } from "../../../pages/listingContentTypes.pages";
import { ContentTypeFormPage } from "../../../pages/contentTypeForm.page";
import { NewEditContentFormPage } from "../../../pages/newEditContentForm.page";
import { updateFeatureFlag } from "../../../utils/api";
import { dotCMSUtils } from "../../../utils/dotCMSUtils";

const contentTypeName = faker.lorem.word().toLocaleLowerCase();

test.beforeEach("Navigate to content types", async ({ page, request }) => {
  const cmsUtils = new dotCMSUtils();
  const listingContentTypesPage = new ListingContentTypesPage(page);
  const contentTypeFormPage = new ContentTypeFormPage(page);

  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  await cmsUtils.login(page, username, password);
  
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.addNewContentType(contentTypeName);
  await contentTypeFormPage.fillNewContentType();
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.goToAddNewContentType(contentTypeName);

  await updateFeatureFlag(request, {
    key: "DOT_FEATURE_FLAG_NEW_EDIT_PAGE",
    value: true,
  });
  await updateFeatureFlag(request, {
    key: "DOT_CONTENT_EDITOR2_ENABLED",
    value: true,
  });
});

test.afterEach(async ({ page, request }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page);
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.deleteContentType(contentTypeName);

  await updateFeatureFlag(request, {
    key: "DOT_FEATURE_FLAG_NEW_EDIT_PAGE",
    value: false,
  });
  await updateFeatureFlag(request, {
    key: "DOT_CONTENT_EDITOR2_ENABLED",
    value: false,
  });
});

test.describe("text field", () => {
  test("should save a text field", async ({ page }) => {
    const newEditContentFormPage = new NewEditContentFormPage(page);

    const locatorField = page.getByTestId("textField");

    await expect(locatorField).toBeVisible();

    const textFieldValue = faker.lorem.word();

    await newEditContentFormPage.fillTextField(textFieldValue);
    await newEditContentFormPage.save();

    await expect(locatorField).toHaveValue(textFieldValue);
  });
});
