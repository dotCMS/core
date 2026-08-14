import { faker } from '@faker-js/faker';
import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createDotAsset, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { getDefaultSite } from '@requests/sites';
import {
    createFakePayloadImageField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { ImageField } from './helpers/image-field';

import { createTestPngFile, createTestTextFile } from '../helpers/file-test-data';

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

test('upload image shows Edit image button', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
    await field.expectVisible();
    await field.uploadFile(TEST_IMAGE);
    // Image fields expose the image editor for images (#36363).
    await field.expectEditButtonVisible();
});

test.describe('required image field', () => {
    let requiredContentType: ContentType;
    let requiredContentTypeVariable: string;

    test.beforeEach(async ({ request }) => {
        requiredContentType = await createImageFieldContentType(request, { required: true });
        requiredContentTypeVariable = requiredContentType.variable;
    });

    test.afterEach(async ({ request }) => {
        await deleteContentType(request, requiredContentType.id);
    });

    test('required empty image field shows error helper text on save', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(requiredContentTypeVariable);

        const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        await formPage.fillTextField(`E2E Required Image ${faker.lorem.word()}`);
        await page.getByRole('button', { name: 'Save' }).click();

        await field.expectRequiredErrorVisible();
        await expect(page).not.toHaveURL(/\/content\/[a-f0-9-]+/);
    });
});

test.describe('select an existing image through the AssetPicker', () => {
    let seededImage: Contentlet | null = null;
    let seededTextFile: Contentlet | null = null;
    let imageName: string;
    let textFileName: string;

    // Two assets on purpose: the image is what the field can take, the text file is what it must
    // refuse to offer. Both seeded through the REST API with unique names so the picker's search
    // reaches exactly these regardless of what else lives in the environment.
    test.beforeEach(async ({ request }) => {
        const site = await getDefaultSite(request);
        const suffix = uniqueSuffix();

        seededImage = await createDotAsset(
            request,
            createTestPngFile(`e2e-picker-${suffix}.png`),
            site.identifier
        );
        seededTextFile = await createDotAsset(
            request,
            createTestTextFile(`e2e-picker-${suffix}.txt`),
            site.identifier
        );

        imageName = seededImage.title;
        textFileName = seededTextFile.title;
    });

    test.afterEach(async ({ request }) => {
        const identifiers = [seededImage, seededTextFile]
            .filter((asset): asset is Contentlet => !!asset)
            .map((asset) => asset.identifier);

        if (identifiers.length) {
            await deleteContentlets(request, identifiers);
        }

        seededImage = null;
        seededTextFile = null;
    });

    test('open the picker, select an image, and populate the field @critical', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();
        await picker.expectConfirmDisabled();

        await picker.searchFor(imageName);
        await picker.expectRowVisible(imageName);

        await picker.selectRowByTitle(imageName);
        await picker.expectRowSelected(imageName);

        await picker.confirm();

        await picker.expectClosed();
        await field.expectPreviewVisible();
        await field.expectThumbnailVisible();
        await field.expectPreviewShowsFileName(imageName);
    });

    test('picker for an image field offers images but not other files', async ({ page }) => {
        // The mimetype restriction is applied silently and cannot be cleared from the UI — an Image
        // field that could return a .txt is broken.
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        // Both assets share the suffix, so one search surfaces whichever the picker is willing
        // to offer.
        const sharedTerm = imageName.replace(/\.png$/, '');
        await picker.searchFor(sharedTerm);

        await picker.expectRowVisible(imageName);
        await expect(picker.row(textFileName)).toHaveCount(0);
    });
});

test('image field shows Generate With dotAI and hides Create New File @smoke', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    const field = new ImageField(page, IMAGE_FIELD_VARIABLE);
    await field.expectVisible();

    await field.expectGenerateWithAiVisible();
    await field.expectCreateNewFileHidden();
});
