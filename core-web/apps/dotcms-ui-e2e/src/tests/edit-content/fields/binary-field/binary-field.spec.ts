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

import { E2E_IMPORT_URL, createTestTextFile } from '../../helpers/file-test-data';

const BINARY_FIELD_VARIABLE = 'binaryField';
const TEST_FILE = createTestTextFile();

let contentType: ContentType | null = null;
let contentTypeVariable: string;

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

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({ request }) => {
    contentType = await createBinaryFieldContentType(request);
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('attach a binary file, save, reload, and file name retained @critical', async ({ page }) => {
    const title = `E2E Binary ${faker.lorem.word()}`;
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

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

test('import from URL completes without 400 and shows preview', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
    await field.expectVisible();
    await field.importFromUrl(E2E_IMPORT_URL);
});

test('required empty binary field shows error helper text on save', async ({ page, request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }

    contentType = await createBinaryFieldContentType(request, { required: true });
    contentTypeVariable = contentType.variable;

    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
    await field.expectVisible();

    await formPage.fillTextField(`E2E Required Binary ${faker.lorem.word()}`);
    await page.getByRole('button', { name: 'Save' }).click();

    await field.expectRequiredErrorVisible();
    await expect(page).not.toHaveURL(/\/content\/[a-f0-9-]+/);
});

test('disabled Generate With dotAI button shows tooltip when AI plugin not installed', async ({
    page
}) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new BinaryField(page, BINARY_FIELD_VARIABLE);
    await field.expectVisible();

    const aiEnabled = await field.isAiButtonEnabled();
    test.skip(aiEnabled, 'dotAI plugin is installed — disabled-tooltip case does not apply');

    await field.expectAiButtonDisabledWithTooltip();
});
