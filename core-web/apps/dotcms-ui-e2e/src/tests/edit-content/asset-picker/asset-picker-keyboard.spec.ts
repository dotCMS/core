import { NewEditContentFormPage } from '@pages';
import { type Page } from '@playwright/test';

import { AssetPickerDialog } from '@components/asset-picker-dialog.component';

import { expect, test } from '../../../fixtures/asset-picker.fixture';
import { ImageField } from '../fields/file-upload-fields/image-field/helpers/image-field';

/**
 * Journey: the keybindings inside the AssetPicker (#32591).
 *
 * The picker gets the shortcuts by consuming the same registry and the same shared listing as
 * Content Drive, which is the whole point of putting them in `libs/ui`. That inheritance is exactly
 * what a unit test cannot confirm: the picker is a dialog stacked over another surface, and the
 * interesting question is which of the *three* search boxes alive at that moment receives the key.
 *
 * The picker also has two search fields of its own — the asset search and the sidebar's folder
 * search — so "focuses the search box" is ambiguous here in a way it never is in Content Drive.
 */
const IMAGE_FIELD_VARIABLE = 'image';

test.describe('AssetPicker keyboard', () => {
    let contentTypeId: string;
    let contentTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix, adminPage }) => {
        // The picker reopens on the globally remembered location; clearing it keeps these tests
        // independent of whatever ran before them.
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

    /** Opens the picker from an image field and returns it, ready to drive. */
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

    /**
     * The asset search, not the sidebar's folder search. With two boxes on screen the shortcut has
     * to pick one, and the asset list is what the dialog is for.
     */
    test('focuses the asset search, not the folder search @critical', async ({ adminPage }) => {
        const picker = await openPicker(adminPage);

        await expect(picker.search).not.toBeFocused();

        await adminPage.keyboard.press('/');

        await expect(picker.search).toBeFocused();
        await expect(picker.folderSearch).not.toBeFocused();
    });

    test('focuses the asset search from the alias too', async ({ adminPage }) => {
        const picker = await openPicker(adminPage);

        await adminPage.keyboard.press('Control+k');

        await expect(picker.search).toBeFocused();
    });

    /**
     * The registry's typing rule, inherited. A synthesised event cannot show this: only a real
     * browser turns the keypress into a character in the field.
     */
    test('types a slash into the asset search instead of re-firing @critical', async ({
        adminPage
    }) => {
        const picker = await openPicker(adminPage);

        await adminPage.keyboard.press('/');
        await expect(picker.search).toBeFocused();

        await adminPage.keyboard.type('a/b');

        await expect(picker.search).toHaveValue('a/b');
    });

    /**
     * Typing in the *sidebar's* box must not be hijacked either. This is the case a single
     * `document` listener gets wrong: the shortcut is claimed, the user is typing somewhere else in
     * the same dialog, and the key has to stay a character.
     */
    test('leaves a slash typed in the folder search alone @critical', async ({ adminPage }) => {
        const picker = await openPicker(adminPage);

        await picker.folderSearch.click();
        await adminPage.keyboard.type('a/b');

        await expect(picker.folderSearch).toHaveValue('a/b');
        await expect(picker.search).not.toBeFocused();
    });

    /**
     * The shared listing's roving tab stop, inherited by the picker. It runs in single-selection
     * mode here, so focus movement is the part that applies; range extension is Content Drive's.
     */
    test('moves focus through the asset list with the arrow keys @critical', async ({
        adminPage
    }) => {
        const picker = await openPicker(adminPage);

        await expect(picker.rows.first()).toBeVisible({ timeout: 20000 });
        const rowCount = await picker.rows.count();
        expect(rowCount).toBeGreaterThan(1);

        // Exactly one row in the tab order, which is what makes the listing reachable at all.
        await expect(picker.list.locator('[data-testid="item-row"][tabindex="0"]')).toHaveCount(1);

        await picker.rows.first().focus();
        await adminPage.keyboard.press('ArrowDown');

        await expect(picker.rows.nth(1)).toBeFocused();

        await adminPage.keyboard.press('ArrowUp');

        await expect(picker.rows.first()).toBeFocused();
    });
});
