import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createDotAsset, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { getDefaultSite } from '@requests/sites';
import {
    createFakePayloadBlockEditorField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { BlockEditorField } from './helpers/block-editor-field';

import {
    createTestPngFile,
    createTestTextFile
} from '../file-upload-fields/helpers/file-test-data';

const BLOCK_EDITOR_FIELD_VARIABLE = 'blockEditorField';

let contentType: ContentType | null = null;
let contentTypeVariable: string;

let seededImage: Contentlet | null = null;
let seededTextFile: Contentlet | null = null;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2EBlockEditorField${uniqueSuffix()}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadBlockEditorField({
                name: 'Block Editor Field',
                variable: BLOCK_EDITOR_FIELD_VARIABLE,
                sortOrder: 2
            })
        ]
    });
    contentTypeVariable = contentType.variable;

    // Two assets on purpose: the image is what the picker must offer, the text file is what its
    // mimetype restriction must hide. Seeded immediately before the test and named uniquely, so they
    // are the newest rows in the picker's default `modDate:desc` listing.
    const site = await getDefaultSite(request);
    const suffix = uniqueSuffix();

    seededImage = await createDotAsset(
        request,
        createTestPngFile(`e2e-block-editor-${suffix}.png`),
        site.identifier
    );
    seededTextFile = await createDotAsset(
        request,
        createTestTextFile(`e2e-block-editor-${suffix}.txt`),
        site.identifier
    );
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }

    const identifiers = [seededImage, seededTextFile]
        .filter((asset): asset is Contentlet => !!asset)
        .map((asset) => asset.identifier);

    if (identifiers.length) {
        await deleteContentlets(request, identifiers);
    }

    seededImage = null;
    seededTextFile = null;
});

test.describe('Block Editor — insert an image through the AssetPicker', () => {
    test('open the picker from the toolbar, select an image, and embed it @critical', async ({
        page
    }) => {
        const image = seededImage as Contentlet;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new BlockEditorField(page, BLOCK_EDITOR_FIELD_VARIABLE);
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
        await field.expectImageInserted(image.inode, image.title);
    });

    test('the picker the Story Block opens is the same one the File field opens', async ({
        page
    }) => {
        // The point of the unification: one picker, with its folder tree and its own header, rather
        // than the older browser-selector this used to open.
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new BlockEditorField(page, BLOCK_EDITOR_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();

        await picker.expectTitle('Add Image');
        await expect(picker.sidebar).toBeVisible();
        await expect(picker.treeSearch).toBeVisible();
        // A row here exists to be picked, not managed.
        await picker.expectNoRowActions();
    });

    test('picker for an image block offers images but not other files', async ({ page }) => {
        // The mimetype restriction is applied silently and cannot be cleared from the UI — a
        // `dotImage` node pointing at a .txt is broken.
        const image = seededImage as Contentlet;
        const textFile = seededTextFile as Contentlet;

        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new BlockEditorField(page, BLOCK_EDITOR_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();

        // Both assets are the newest in the environment, so the picker's own listing is where
        // this shows: one of them is offered and the other never appears.
        await picker.expectRowVisible(image.title);
        await expect(picker.row(textFile.title)).toHaveCount(0);
    });

    test('cancelling the picker embeds nothing', async ({ page }) => {
        const image = seededImage as Contentlet;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new BlockEditorField(page, BLOCK_EDITOR_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();

        // Select first: cancelling after a selection is the case that would leak a node if the
        // dialog reported a result on dismiss.
        await picker.expectRowVisible(image.title);
        await picker.selectRowByTitle(image.title);
        await picker.cancel();

        await picker.expectClosed();
        await field.expectNoImages();
    });

    test('the embedded image survives a save and reload @critical', async ({ page }) => {
        // The only test here that persists anything, and the 60s budget covers the hooks too: the
        // seed uploads two assets, the body pays for the picker *and* a second full dotAdmin boot
        // after the save, and cleanup then deletes a content type that now has content in it.
        // Teardown was what ran out of time on CI, not the assertions.
        test.slow();

        const image = seededImage as Contentlet;
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new BlockEditorField(page, BLOCK_EDITOR_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(page);
        await field.openImagePicker();
        await picker.waitForVisible();
        await picker.expectRowVisible(image.title);
        await picker.selectRowByTitle(image.title);
        await picker.confirm();
        await picker.expectClosed();
        await field.expectImageInserted(image.inode, image.title);

        await formPage.fillTextField(`E2E Block Editor ${uniqueSuffix()}`);
        await formPage.save();

        await page.waitForURL(/\/content\/([a-f0-9-]+)/);
        const [, savedIdentifier] = page.url().match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
        expect(savedIdentifier).toBeTruthy();

        await page.goto(`/dotAdmin/#/content/${savedIdentifier}`);
        await page.waitForLoadState('domcontentloaded');
        await page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });

        // What this really covers is the node's stored `data` payload: the picker hands back a
        // hydrated contentlet, and the node keeps only identifier/inode/languageId/title/asset from
        // it. If any of those went missing the reloaded document would render a broken image.
        await field.expectVisible();
        await field.expectImageInserted(image.inode, image.title);
    });
});
