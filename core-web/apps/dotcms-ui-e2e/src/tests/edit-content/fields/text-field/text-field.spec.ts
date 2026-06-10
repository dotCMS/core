import { faker } from '@faker-js/faker';
import { ListingContentPage, NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

import { createFakeTextField } from '@dotcms/utils-testing';

let contentType: ContentType | null = null;
let contentTypeVariable: string;

test.beforeEach(async ({ request }) => {
    const titleField = createFakeTextField({
        name: 'Title',
        variable: 'title',
        sortOrder: 1
    });

    contentType = await createFakeContentType(request, {
        name: `E2ETextField${Date.now()}`,
        fields: [titleField]
    });
    contentTypeVariable = contentType.variable;
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
    await formPage.goToNew(contentTypeVariable);

    const titleLocator = page.getByTestId('title');
    await expect(titleLocator).toBeVisible();

    await titleLocator.fill(textFieldValue);
    await formPage.save();

    const listingPage = new ListingContentPage(page);
    await listingPage.goTo(contentTypeVariable);
    await listingPage.clickFirstContentRow();

    await expect(titleLocator).toHaveValue(textFieldValue);
});
