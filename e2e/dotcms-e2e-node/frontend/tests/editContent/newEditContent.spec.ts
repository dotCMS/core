import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import { ListingContentTypesPage } from "../../pages/listingContentTypes.pages";
import { ContentTypeFormPage } from "../../pages/contentTypeForm.page";
import { NewEditContentFormPage } from "../../pages/newEditContentForm.page";

const contentTypeName = faker.lorem.word().toLocaleLowerCase();

test.beforeEach("Navigate to content types", async ({ page }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page);
  const contentTypeFormPage = new ContentTypeFormPage(page);
  await listingContentTypesPage.goTo();
  await listingContentTypesPage.addNewContentType(contentTypeName);
  await contentTypeFormPage.fillNewContentType();
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.goToAddNewContentType(contentTypeName);
});

test.afterEach(async ({ page }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page);
  await listingContentTypesPage.goToUrl();
  await listingContentTypesPage.deleteContentType(contentTypeName);
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
