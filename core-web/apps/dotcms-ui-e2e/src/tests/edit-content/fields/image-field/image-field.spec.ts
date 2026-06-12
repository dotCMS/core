import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadImageField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { ImageField } from './helpers/image-field';

import { createTestPngFile } from '../../helpers/file-test-data';

const IMAGE_FIELD_VARIABLE = 'imageField';
const TEST_IMAGE = createTestPngFile();

let contentType: ContentType | null = null;
let contentTypeVariable: string;

async function createImageFieldContentType(
    request: Parameters<typeof createFakeContentType>[0],
    options: { required?: boolean } = {}
) {
    return createFakeContentType(request, {
        name: `E2EImageField${uniqueSuffix()}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadImageField({
                name: 'Image Field',
                variable: IMAGE_FIELD_VARIABLE,
                sortOrder: 2,
                required: options.required ?? false
            })
        ]
    });
}

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({ request }) => {
    contentType = await createImageFieldContentType(request);
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('upload an image, save, reload, and thumbnail still displayed @critical', async ({ page }) => {
    const title = `E2E Image ${faker.lorem.word()}`;
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
    await field.expectVisible();
    await field.uploadFile(TEST_IMAGE);

    await formPage.fillTextField(title);
    await formPage.save();

    await page.waitForURL(/\/content\/([a-f0-9-]+)/);
    const [, savedContentIdentifier] = page
        .url()
        .match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
    expect(savedContentIdentifier).toBeTruthy();

    await page.goto(`/dotAdmin/#/content/${savedContentIdentifier}`);
    await page.waitForLoadState('domcontentloaded');
    await page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });

    await field.expectPreviewVisible();
    await field.expectThumbnailVisible();
    await field.expectPreviewShowsFileName(TEST_IMAGE.name);
});

test('required empty image field shows error helper text on save', async ({ page, request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }

    contentType = await createImageFieldContentType(request, { required: true });
    contentTypeVariable = contentType.variable;

    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
    await field.expectVisible();

    await formPage.fillTextField(`E2E Required Image ${faker.lorem.word()}`);
    await page.getByRole('button', { name: 'Save' }).click();

    await field.expectRequiredErrorVisible();
    await expect(page).not.toHaveURL(/\/content\/[a-f0-9-]+/);
});

test('image field shows Generate With dotAI and hides Create New File @smoke', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
    await field.expectVisible();

    await field.expectGenerateWithAiVisible();
    await field.expectCreateNewFileHidden();
});
