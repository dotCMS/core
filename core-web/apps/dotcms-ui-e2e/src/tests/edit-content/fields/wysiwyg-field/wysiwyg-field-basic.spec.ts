import { NewEditContentFormPage } from '@pages';
import { test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadTextField,
    createFakePayloadWYSIWYGField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { WysiwygField } from './helpers/wysiwyg-field';

/**
 * A simple smoke check for the WYSIWYG field's dotCMS integration — not a TinyMCE feature test.
 * The `wysiwyg-field-asset-picker.spec.ts` suite already exercises the AssetPicker in depth; this
 * file only proves the field renders, the editor loads, text can be typed, and it survives a save
 * and reload.
 */
const WYSIWYG_FIELD_VARIABLE = 'wysiwygField';

let contentType: ContentType | null = null;
let contentTypeVariable: string;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2EWysiwygBasic${uniqueSuffix()}`,
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
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test.describe('WYSIWYG — basic editing', () => {
    test('the editor loads and accepts typed text @critical', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();

        await field.typeText('Hello from the e2e smoke test');
        await field.expectText('Hello from the e2e smoke test');
    });

    test('typed text survives a save and reload @critical', async ({ page }) => {
        const formPage = new NewEditContentFormPage(page);
        await formPage.goToNew(contentTypeVariable);

        const field = new WysiwygField(page, WYSIWYG_FIELD_VARIABLE);
        await field.expectVisible();
        await field.typeText('Persisted WYSIWYG text');

        await formPage.fillTextField(`E2E WYSIWYG Basic ${uniqueSuffix()}`);
        await formPage.save();

        await page.waitForURL(/\/content\/([a-f0-9-]+)/);
        const [, savedIdentifier] = page.url().match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;

        await page.goto(`/dotAdmin/#/content/${savedIdentifier}`);
        await page.waitForLoadState('domcontentloaded');
        await page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });

        await field.expectVisible();
        await field.expectText('Persisted WYSIWYG text');
    });
});
