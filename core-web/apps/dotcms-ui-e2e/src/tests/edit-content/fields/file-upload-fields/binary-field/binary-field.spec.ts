import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadBinaryField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { BinaryField } from './helpers/binary-field';

import { E2E_IMPORT_URL, createTestTextFile } from '../helpers/file-test-data';

const BINARY_FIELD_VARIABLE = 'binaryField';
const TEST_FILE = createTestTextFile();

async function createBinaryFieldContentType(
    request: Parameters<typeof createFakeContentType>[0],
    options: { required?: boolean } = {}
) {
    return createFakeContentType(request, {
        name: `E2EBinaryField${uniqueSuffix()}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadBinaryField({
                name: 'Binary Field',
                variable: BINARY_FIELD_VARIABLE,
                sortOrder: 2,
                required: options.required ?? false
            })
        ]
    });
}

async function withBinaryContentType(
    request: Parameters<typeof createFakeContentType>[0],
    options: { required?: boolean },
    run: (contentType: ContentType) => Promise<void>
) {
    const contentType = await createBinaryFieldContentType(request, options);
    try {
        await run(contentType);
    } finally {
        await deleteContentType(request, contentType.id);
    }
}

test('attach a binary file, save, reload, and file name retained @critical', async ({
    page,
    request
}) => {
    await withBinaryContentType(request, {}, async (contentType) => {
        const title = `E2E Binary ${faker.lorem.word()}`;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.uploadFile(TEST_FILE);

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
        await field.expectPreviewShowsContent('dotCMS E2E test file content');
    });
});

test('import from URL completes without 400 and shows preview', async ({ page, request }) => {
    await withBinaryContentType(request, {}, async (contentType) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.importFromUrl(E2E_IMPORT_URL);
    });
});

test.describe('required binary field', () => {
    test('required empty binary field shows error helper text on save', async ({
        page,
        request
    }) => {
        await withBinaryContentType(request, { required: true }, async (contentType) => {
            const formPage = new NewEditContentFormPage(page);
            await formPage.goToNew(contentType.variable);

            const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
            await field.expectVisible();

            await formPage.fillTextField(`E2E Required Binary ${faker.lorem.word()}`);
            await page.getByRole('button', { name: 'Save' }).click();

            await field.expectRequiredErrorVisible();
            await expect(page).not.toHaveURL(/\/content\/[a-f0-9-]+/);
        });
    });
});

test('upload text file does not show Edit image button', async ({ page, request }) => {
    await withBinaryContentType(request, {}, async (contentType) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.uploadFile(TEST_FILE);
        await field.expectEditButtonHidden();
    });
});

test('remove file shows confirm popup and clears preview', async ({ page, request }) => {
    await withBinaryContentType(request, {}, async (contentType) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.uploadFile(TEST_FILE);
        await field.expectPreviewVisible();

        await field.clickRemoveButton();
        await field.confirmRemoveInPopup();
        await field.expectPreviewHidden();
    });
});

test('edit existing binary contentlet shows preview without server error', async ({
    page,
    request
}) => {
    await withBinaryContentType(request, {}, async (contentType) => {
        const title = `E2E Binary Image Hydration ${faker.lorem.word()}`;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.importFromUrl(E2E_IMPORT_URL);

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
        await field.expectNoServerErrorMessage();
        await field.expectThumbnailVisible();
    });
});

test('disabled Generate With dotAI button shows tooltip when AI plugin not installed', async ({
    page,
    request
}) => {
    await withBinaryContentType(request, {}, async (contentType) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentType.variable);

        const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
        await field.expectVisible();
        await field.expectAiButtonDisabledWithTooltipWhenApplicable();
    });
});
