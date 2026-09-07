import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';

let contentType: ContentType | null = null;
let contentTypeVariable: string;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2ETextField${Date.now()}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            })
        ]
    });
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('save reloads with content id in URL and persists text field value @critical', async ({
    page
}) => {
    const textFieldValue = faker.lorem.word();

    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const titleLocator = page.getByTestId('title');
    await expect(titleLocator).toBeVisible();

    await titleLocator.fill(textFieldValue);
    await formPage.save();

    await page.waitForURL(/\/content\/([a-f0-9-]+)/);
    const [, savedContentIdentifier] = page
        .url()
        .match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
    expect(savedContentIdentifier).toBeTruthy();

    await expect(titleLocator).toHaveValue(textFieldValue);
});
