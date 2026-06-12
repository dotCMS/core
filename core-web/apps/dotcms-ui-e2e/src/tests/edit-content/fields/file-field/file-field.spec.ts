import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadFileField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { FileField } from './helpers/file-field';

import { E2E_IMPORT_URL, createTestTextFile } from '../../helpers/file-test-data';

const FILE_FIELD_VARIABLE = 'fileField';
const TEST_FILE = createTestTextFile();

let contentType: ContentType | null = null;
let contentTypeVariable: string;

async function createFileFieldContentType(
    request: Parameters<typeof createFakeContentType>[0],
    options: { required?: boolean } = {}
) {
    return createFakeContentType(request, {
        name: `E2EFileField${uniqueSuffix()}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadFileField({
                name: 'File Field',
                variable: FILE_FIELD_VARIABLE,
                sortOrder: 2,
                required: options.required ?? false
            })
        ]
    });
}

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({ request }) => {
    contentType = await createFileFieldContentType(request);
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('upload a file, save, reload, and file still shown @critical', async ({ page }) => {
    const title = `E2E File ${faker.lorem.word()}`;
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new FileField(page, FILE_FIELD_VARIABLE);
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

test('import from URL dialog opens with header, footer, and close button', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new FileField(page, FILE_FIELD_VARIABLE);
    await field.expectVisible();

    const dialog = await field.openImportFromUrlDialog();
    const { cancelButton, importButton, urlInput } = field.getImportDialogLocators();

    await expect(dialog).toContainText('URL');
    await expect(urlInput).toBeVisible();
    await expect(cancelButton).toBeVisible();
    await expect(importButton).toBeVisible();

    await field.closeImportDialogViaX();
});

test('import from URL completes without 400 and shows preview @critical', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new FileField(page, FILE_FIELD_VARIABLE);
    await field.expectVisible();
    await field.importFromUrl(E2E_IMPORT_URL);
});

test('required empty file field shows error helper text on save', async ({ page, request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }

    contentType = await createFileFieldContentType(request, { required: true });
    contentTypeVariable = contentType.variable;

    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new FileField(page, FILE_FIELD_VARIABLE);
    await field.expectVisible();

    await formPage.fillTextField(`E2E Required File ${faker.lorem.word()}`);
    await page.getByRole('button', { name: 'Save' }).click();

    await field.expectRequiredErrorVisible();
    await expect(page).not.toHaveURL(/\/content\/[a-f0-9-]+/);
});
