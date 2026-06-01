import { faker } from '@faker-js/faker';
import { ListingContentPage, NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { createFakeContentType, deleteContentType, ContentType } from '@requests/contentType';

let contentType: ContentType | null = null;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2ETextField${Date.now()}`
    });
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('save and persist a text field value @critical', async ({ page }) => {
    const textFieldValue = faker.lorem.word();

    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentType.variable);

    const titleLocator = page.getByTestId('title');
    await expect(titleLocator).toBeVisible();

    await titleLocator.fill(textFieldValue);
    await formPage.save();

    const listingPage = new ListingContentPage(page);
    await listingPage.goTo(contentType.variable);
    await listingPage.clickFirstContentRow();

    await expect(titleLocator).toHaveValue(textFieldValue);
});
