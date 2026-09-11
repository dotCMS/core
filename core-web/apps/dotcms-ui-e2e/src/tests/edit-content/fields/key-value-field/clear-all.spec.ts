import { ContentTypeBuilderPage } from '@pages';
import { expect, test, type Locator, type Page } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadKeyValueField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

import { KeyValueField } from './helpers/key-value-field';

const APP_KEY = 'dotVelocitySecretApp';

/** PrimeNG appends the dialog to the body, so it is located globally. */
const confirmClear = async (page: Page, root: Locator) => {
    await root.getByTestId('dot-key-value-clear-all').click();
    const accept = page.locator('.p-confirmdialog-accept-button');
    await expect(accept).toBeVisible({ timeout: 5000 });
    await accept.click();
    await expect(accept).toHaveCount(0);
};

/**
 * Clear All in each of the three consumers (#37191).
 *
 * All three, because the failure was per-host: the editor asked `ConfirmationService`
 * for a confirmation and dotcms-ui had no dialog listening — only Edit Content's layout
 * renders one unconditionally — so Clear All silently did nothing in Field Variables
 * and Apps. A unit test cannot see that; only mounting each host can.
 */
/**
 * Opens the first site's configuration and fills the one parameter it requires, which
 * the app form needs before its own Save is enabled.
 */
async function openAppConfiguration(page: import('@playwright/test').Page) {
    await page.goto(`/dotAdmin/#/apps/${APP_KEY}`);
    await page.waitForLoadState('domcontentloaded');

    const first = page.locator('dot-apps-configuration-item').first();
    await first.waitFor({ timeout: 20000 });
    await first
        .getByTestId(/^(edit|add)$/)
        .first()
        .click();

    const panel = page.locator('dot-apps-configuration-detail');
    await panel.waitFor({ timeout: 20000 });

    const required = panel.getByTestId('title').locator('input, textarea').first();
    if ((await required.count()) && !(await required.inputValue())) {
        await required.fill('e2e');
    }

    return panel;
}

test.describe('clear all across consumers @smoke', () => {
    let ct: ContentType | null = null;
    let ctId = '';
    let variable = '';

    test.beforeEach(async ({ request }) => {
        ct = await createFakeContentType(request, {
            name: `E2EClear${uniqueSuffix()}`,
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadKeyValueField({
                    name: 'KV',
                    variable: 'keyValueField',
                    sortOrder: 2
                })
            ]
        });
        ctId = ct.id;
        variable = ct.variable;
    });

    test.afterEach(async ({ request }) => {
        if (ct) {
            await deleteContentType(request, ct.id);
            ct = null;
        }
    });

    test('Edit Content', async ({ page }) => {
        await page.goto(`/dotAdmin/#/content/new/${variable}`);
        await page.waitForLoadState('domcontentloaded');
        await page.getByTestId('title').waitFor({ state: 'visible', timeout: 20000 });
        const f = new KeyValueField(page, 'keyValueField');
        await f.expectVisible();
        const root = page.getByTestId('field-keyValueField');
        await f.addEntry('a', '1');
        await f.addEntry('b', '2');

        await confirmClear(page, root);
        await expect(root.getByTestId('dot-key-value-key')).toHaveCount(0);
    });

    test('Field Variables', async ({ page }) => {
        const builder = new ContentTypeBuilderPage(page);
        await builder.goToBuilder(ctId);
        await builder.openFieldVariables('Title');
        const panel = builder.fieldVariablesPanel();
        const f = new KeyValueField(page, panel);
        await f.expectVisible();
        await f.addEntry('a', '1');
        await f.addEntry('b', '2');

        await confirmClear(page, panel);
        await expect(panel.getByTestId('dot-key-value-key')).toHaveCount(0);
    });

    test('Apps', async ({ page }) => {
        const panel = await openAppConfiguration(page);
        const f = new KeyValueField(page, panel);
        await f.expectVisible();
        await f.addEntry(`x${uniqueSuffix()}`, '1');

        await confirmClear(page, panel);
        await expect(panel.getByTestId('dot-key-value-key')).toHaveCount(0);
    });
});
