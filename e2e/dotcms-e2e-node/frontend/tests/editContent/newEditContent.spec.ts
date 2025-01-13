import { test, expect } from "@playwright/test";
import { faker } from "@faker-js/faker";
import { ListingContentTypesPage } from "../../pages/listingContentTypes.pages";
import { ContentTypeFormPage } from "../../pages/contentTypeForm.page";
import { NewEditContentFormPage } from "../../pages/newEditContentForm.page";

test.beforeEach("Navigate to content types", async ({ page }) => {
  const listingContentTypesPage = new ListingContentTypesPage(page);
  const contentTypeFormPage = new ContentTypeFormPage(page);
  await listingContentTypesPage.goTo();

  const contentTypeName = faker.lorem.word().toLocaleLowerCase();

  await listingContentTypesPage.addNewContentType(contentTypeName);
  await contentTypeFormPage.fillNewContentType();
  await listingContentTypesPage.gotToContentTypes();
  await listingContentTypesPage.gotToContentType(contentTypeName);
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
