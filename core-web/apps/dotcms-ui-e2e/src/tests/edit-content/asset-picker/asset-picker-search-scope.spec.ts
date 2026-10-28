import { NewEditContentFormPage } from '@pages';
import { type Page } from '@playwright/test';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { expect, test } from '../../../fixtures/asset-picker.fixture';
import { ImageField } from '../fields/file-upload-fields/image-field/helpers/image-field';

/**
 * Journey: the AssetPicker's search bar offers the same Title / All Fields scope as Content Drive
 * (#38056). It opens on Title every time, and a search names its scope in the request.
 */
const IMAGE_FIELD_VARIABLE = 'image';

test.describe('AssetPicker search scope', () => {
    let contentTypeId: string;
    let contentTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix, adminPage }) => {
        await adminPage.addInitScript(() =>
            window.localStorage.removeItem('dotcms.asset-picker.lastPath')
        );

        const contentType = await apiHelpers.createContentType(
            apiHelpers.assetPickerPayload(testSuffix)
        );
        contentTypeId = contentType.id;
        contentTypeVariable = contentType.variable;
    });

    test.afterEach(async ({ apiHelpers }) => {
        await apiHelpers.deleteContentType(contentTypeId);
    });

    async function openPicker(adminPage: Page): Promise<AssetPickerDialog> {
        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToNew(contentTypeVariable);

        const field = new ImageField(adminPage, IMAGE_FIELD_VARIABLE);
        await field.expectVisible();

        const picker = new AssetPickerDialog(adminPage);
        await field.openSelectExistingDialog();
        await picker.waitForVisible();

        return picker;
    }

    test('opens on Title and sends the scope with a term @critical', async ({ adminPage }) => {
        const picker = await openPicker(adminPage);

        await picker.expectSearchScope('Title');
        expect(await picker.searchScopeSentFor('logo')).toBe('TITLE');
    });

    test('sends All Fields once the editor picks it, and opens on Title again next time', async ({
        adminPage
    }) => {
        const picker = await openPicker(adminPage);

        await picker.chooseSearchScope('ALL_FIELDS');
        await picker.expectSearchScope('All Fields');
        expect(await picker.searchScopeSentFor('logo')).toBe('ALL_FIELDS');

        await picker.cancel();
        await picker.expectClosed();

        const reopened = await openPicker(adminPage);
        await reopened.expectSearchScope('Title');
    });

    test('keeps the folder search free of a scope', async ({ adminPage }) => {
        const picker = await openPicker(adminPage);

        await expect(picker.sidebar.getByTestId('search-scope-trigger')).toHaveCount(0);
        await expect(picker.folderSearch).toBeVisible();
    });
});
