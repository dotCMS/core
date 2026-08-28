import { expect, test } from '@playwright/test';

import { KeyValueField } from '../edit-content/fields/key-value-field/helpers/key-value-field';

/**
 * Apps custom properties — the third consumer of the shared Key/Value editor (#37191).
 *
 * This is the only surface that offers hidden values, so it is the only place
 * the eye affordance and the always-visible hidden indicator can be exercised
 * end to end. Seeded through the UI because app configuration is what is being
 * tested; the editor's own logic is covered by unit tests.
 */
const APP_KEY = 'dotsalesforce';

test.describe('apps — custom properties', () => {
    /** Opens an app configuration that allows extra params. */
    async function openAppConfiguration(page: import('@playwright/test').Page) {
        await page.goto(`/dotAdmin/#/apps/${APP_KEY}`);
        await page.waitForLoadState('domcontentloaded');

        const firstSite = page.getByTestId('configuration-list').locator('li').first();
        await firstSite.waitFor({ state: 'visible', timeout: 15000 });
        await firstSite.click();

        const panel = page.locator('dot-apps-configuration-detail');
        await panel.waitFor({ state: 'visible', timeout: 15000 });

        return panel;
    }

    test('add, mask and persist custom properties @smoke', async ({ page }) => {
        const panel = await openAppConfiguration(page);
        const properties = new KeyValueField(page, panel);
        await properties.expectVisible();

        await properties.addEntry('analyticsId', 'UA-4419-22');
        await properties.addEntry('theme', 'dark');
        await properties.expectEntry('analyticsId', 'UA-4419-22');
        await properties.expectEntry('theme', 'dark');

        await page.getByTestId('saveBtn').click();

        const reopened = await openAppConfiguration(page);
        const reloaded = new KeyValueField(page, reopened);
        await reloaded.expectEntry('analyticsId', 'UA-4419-22');
        await reloaded.expectEntry('theme', 'dark');
    });

    test('the hidden indicator is visible without hovering the row @smoke', async ({ page }) => {
        // FR-018 — the indicator communicates state, so unlike the remove and
        // drag actions it must never be hover-gated. A reviewer looking at a
        // list of properties has to be able to tell which ones are secret
        // without touching the mouse.
        const panel = await openAppConfiguration(page);
        const properties = new KeyValueField(page, panel);
        await properties.expectVisible();

        await panel.getByTestId('dot-key-value-visibility-toggle').click();
        await properties.addEntry('apiToken', 'super-secret-value');

        const row = panel
            .getByTestId('dot-key-value-key')
            .filter({ hasText: /^apiToken$/ })
            .locator('xpath=ancestor::tr[contains(@class,"dot-key-value-table-row")][1]');

        // No hover anywhere before asserting.
        const toggle = row.getByTestId('dot-key-value-visibility-toggle');
        await expect(toggle).toBeVisible();
        await expect(toggle.getByTestId('dot-key-value-visibility-icon')).toHaveText(
            'visibility_off'
        );
        await expect(row.getByTestId('dot-key-value-masked-value')).toBeVisible();
        await expect(row).not.toContainText('super-secret-value');

        // Hiding must be reversible — the same control brings the value back.
        await toggle.click();
        await expect(row.getByTestId('dot-key-value-masked-value')).toHaveCount(0);
        await expect(row).toContainText('super-secret-value');

        // The remove action on the same row stays hidden until hovered.
        await expect(row.getByTestId('dot-key-value-delete-button')).toBeHidden();
        await row.hover();
        await expect(row.getByTestId('dot-key-value-delete-button')).toBeVisible();
    });

    test('rows can be reordered @smoke', async ({ page }) => {
        const panel = await openAppConfiguration(page);
        const properties = new KeyValueField(page, panel);
        await properties.expectVisible();

        await properties.addEntry('alpha', 'first');
        await properties.addEntry('beta', 'second');
        await properties.expectKeyOrder(['beta', 'alpha']);

        await properties.dragRowTo('alpha', 0);
        await properties.expectKeyOrder(['alpha', 'beta']);
    });
});
