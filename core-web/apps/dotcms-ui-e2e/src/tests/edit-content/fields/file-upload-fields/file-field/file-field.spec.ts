import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createDotAsset, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { getDefaultSite } from '@requests/sites';
import {
    createFakePayloadFileField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { FileField } from './helpers/file-field';

import { E2E_IMPORT_URL, createTestPngFile, createTestTextFile } from '../helpers/file-test-data';

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

test('import image URL shows Edit image button', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new FileField(page, FILE_FIELD_VARIABLE);
    await field.expectVisible();
    await field.importFromUrl(E2E_IMPORT_URL);
    // File fields expose the image editor when the file is an image (#36363).
    await field.expectEditButtonVisible();
});

test.describe('select an existing file through the AssetPicker', () => {
    let seededAsset: Contentlet | null = null;
    let assetName: string;

    // Seeded per test through the REST API: the picker only reads it, but a unique file name per
    // test is what lets the search find exactly this asset regardless of what else the environment
    // happens to contain.
    //
    // An image on purpose: the preview renders text assets as an editable code block
    // (`code-preview`) and everything else as thumbnail + metadata, so a .txt here would never
    // produce the file name this test asserts on.
    test.beforeEach(async ({ request }) => {
        const site = await getDefaultSite(request);
        seededAsset = await createDotAsset(
            request,
            createTestPngFile(`e2e-picker-${uniqueSuffix()}.png`),
            site.identifier
        );
        assetName = seededAsset.title;
    });

    test.afterEach(async ({ request }) => {
        if (seededAsset) {
            await deleteContentlets(request, [seededAsset.identifier]);
            seededAsset = null;
        }
    });

    test('open the picker, select a file, and populate the field @critical', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new FileField(page, FILE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        // Nothing picked yet, so there is nothing to confirm.
        await picker.expectConfirmDisabled();

        await picker.searchFor(assetName);
        await picker.expectRowVisible(assetName);

        // Clicking the title, not the row padding: the whole row is the selection target here.
        await picker.selectRowByTitle(assetName);
        await picker.expectRowSelected(assetName);
        await picker.expectConfirmEnabled();

        await picker.confirm();

        await picker.expectClosed();
        await field.expectPreviewVisible();
        await field.expectThumbnailVisible();
        await field.expectPreviewShowsFileName(assetName);
    });

    test('cancel the picker and leave the field untouched', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new FileField(page, FILE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        await picker.searchFor(assetName);
        await picker.selectRowByTitle(assetName);
        await picker.cancel();

        await picker.expectClosed();
        // Highlighting a row and backing out must not populate the field.
        await field.expectPreviewHidden();
    });
});

test.describe('required file field', () => {
    let requiredContentType: ContentType;
    let requiredContentTypeVariable: string;

    test.beforeEach(async ({ request }) => {
        requiredContentType = await createFileFieldContentType(request, { required: true });
        requiredContentTypeVariable = requiredContentType.variable;
    });

    test.afterEach(async ({ request }) => {
        await deleteContentType(request, requiredContentType.id);
    });

    test('required empty file field shows error helper text on save', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(requiredContentTypeVariable);

        const field = new FileField(page, FILE_FIELD_VARIABLE);
        await field.expectVisible();

        await formPage.fillTextField(`E2E Required File ${faker.lorem.word()}`);
        await page.getByRole('button', { name: 'Save' }).click();

        await field.expectRequiredErrorVisible();
        await expect(page).not.toHaveURL(/\/content\/[a-f0-9-]+/);
    });
});
