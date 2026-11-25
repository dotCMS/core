import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createDotAsset, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { getDefaultSite } from '@requests/sites';
import {
    createFakePayloadTextField,
    createFakePayloadWYSIWYGField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { WysiwygField } from './helpers/wysiwyg-field';

import {
    createTestPngFile,
    createTestTextFile
} from '../file-upload-fields/helpers/file-test-data';

const WYSIWYG_FIELD_VARIABLE = 'wysiwygField';

let contentType: ContentType | null = null;
let contentTypeVariable: string;

let seededImage: Contentlet | null = null;
let seededTextFile: Contentlet | null = null;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2EWysiwygField${uniqueSuffix()}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadWYSIWYGField({
                name: 'WYSIWYG Field',
                variable: WYSIWYG_FIELD_VARIABLE,
                sortOrder: 2
            })
        ]
    });
    contentTypeVariable = contentType.variable;

    // The image is what the picker must offer. Seeded immediately before the test and named
    // uniquely, so it is the newest row in the picker's default `modDate:desc` listing. The one test
    // that needs a non-image seeds its own: every asset costs a create here and a destroy in
    // teardown, and both count against the test's timeout.
    const site = await getDefaultSite(request);

    seededImage = await createDotAsset(
        request,
        createTestPngFile(`e2e-wysiwyg-${uniqueSuffix()}.png`),
        site.identifier
    );
});

test.afterEach(async ({ request }) => {
    const identifiers = [seededImage, seededTextFile]
        .filter((asset): asset is Contentlet => !!asset)
        .map((asset) => asset.identifier);

    // Independent of one another, so they run together rather than one after the other.
    await Promise.all([
        contentType ? deleteContentType(request, contentType.id) : undefined,
        ...identifiers.map((identifier) => deleteContentlets(request, [identifier]))
    ]);

    contentType = null;
    seededImage = null;
    seededTextFile = null;
});

test.describe('WYSIWYG — insert an image through the AssetPicker', () => {
    test('open the picker from the toolbar, select an image, and insert it @critical', async ({
        page
    }) => {
        const image = seededImage as Contentlet;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();
        await field.expectNoImages();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();
        await picker.expectConfirmDisabled();

        await picker.expectRowVisible(image.title);

        await picker.selectRowByTitle(image.title);
        await picker.expectRowSelected(image.title);
        await picker.expectConfirmEnabled();

        await picker.confirm();

        await picker.expectClosed();
        // The insert goes through `formatDotImageNode`, which reads far more of the contentlet than
        // the Story Block does — path, extension, hostName and the shorty ids. This is the assertion
        // that proves the hydrated contentlet the picker returns carries all of it.
        await field.expectImageInserted(image.identifier, image.inode, image.title);
    });

    test('the picker the WYSIWYG opens is the same one the File field opens', async ({ page }) => {
        // The point of the unification: one picker, with its folder tree and its own header, rather
        // than the older grid search dialog this used to open.
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();

        await picker.expectTitle('Add Image');
        await expect(picker.sidebar).toBeVisible();
        await expect(picker.folderSearch).toBeVisible();
        await picker.expectNoRowActions();
    });

    test('picker for the WYSIWYG offers images but not other files', async ({ page, request }) => {
        const image = seededImage as Contentlet;
        // What the picker's mimetype restriction must hide, seeded after the image so it is the
        // newest row of all.
        const site = await getDefaultSite(request);
        seededTextFile = await createDotAsset(
            request,
            createTestTextFile(`e2e-wysiwyg-${uniqueSuffix()}.txt`),
            site.identifier
        );
        const textFile = seededTextFile;

        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();

        // Both assets are the newest in the environment, so the picker's own listing is where this
        // shows: one of them is offered and the other never appears.
        await picker.expectRowVisible(image.title);
        await expect(picker.row(textFile.title)).toHaveCount(0);
    });

    test('cancelling the picker inserts nothing and returns focus to the editor', async ({
        page
    }) => {
        const image = seededImage as Contentlet;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();

        // Select first: cancelling after a selection is the case that would leak an <img> if the
        // dialog reported a result on dismiss.
        await picker.expectRowVisible(image.title);
        await picker.selectRowByTitle(image.title);
        await picker.cancel();

        await picker.expectClosed();
        await field.expectNoImages();

        // The plugin refocuses the editor on every close, insert or dismiss, so the user is never
        // left with nothing focused.
        await expect(field.body.locator('body')).toBeFocused();
    });

    test('the inserted image survives a save and reload @critical', async ({ page }) => {
        const image = seededImage as Contentlet;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();
        await picker.expectRowVisible(image.title);
        await picker.selectRowByTitle(image.title);
        await picker.confirm();
        await picker.expectClosed();
        await field.expectImageInserted(image.identifier, image.inode, image.title);

        await formPage.fillTextField(`E2E WYSIWYG ${uniqueSuffix()}`);
        const saved = await formPage.save();

        await formPage.reopen(saved.inode);

        await field.expectVisible();
        await field.expectImageInserted(image.identifier, image.inode, image.title);
    });

    test('the icon-only insert-image button has an accessible name @smoke', async ({ page }) => {
        // It is the only affordance for inserting an image and carries no text, so without a name
        // from the plugin's `tooltip` there is nothing for a screen reader to announce.
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();

        await field.expectInsertImageButtonLabelled();
    });
});
