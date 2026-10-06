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
/**
 * Which app this drives is not arbitrary — two constraints rule most of them out.
 *
 * Save is bound to the whole app form's validity, so an app with many required
 * parameters can only be saved on an instance where they happen to be filled in
 * already. SSO — SAML requires nine, including certificates: it passed locally, where
 * it was configured, and timed out on CI's fresh instance clicking a disabled Save.
 *
 * `dotAI` has none required but is routed to its own screen
 * (`DotAiConfigDetailComponent`), so it never renders the shared editor at all.
 *
 * `dotVelocitySecretApp` uses the generic configuration panel and requires exactly one
 * string, which the spec fills itself. Nothing here depends on how the instance was
 * seeded.
 */
const APP_KEY = 'dotVelocitySecretApp';
const REQUIRED_PARAM = 'title';

/**
 * These tests drive a real, shared app configuration that already holds properties,
 * so every key they create is suffixed per run. That keeps them from colliding with
 * existing data, with each other, or with a previous run of themselves.
 */
const unique = (name: string) => `e2e-${name}-${Date.now().toString(36)}`;

test.describe('apps — custom properties', () => {
    /** Opens the first site's configuration for an app that allows extra params. */
    async function openAppConfiguration(page: import('@playwright/test').Page) {
        await page.goto(`/dotAdmin/#/apps/${APP_KEY}`);
        await page.waitForLoadState('domcontentloaded');

        const firstSite = page.locator('dot-apps-configuration-item').first();
        await firstSite.waitFor({ state: 'visible', timeout: 15000 });

        // A configured site offers `edit`; an unconfigured one offers `add`. Both
        // open the same detail panel, so take whichever this site has.
        await firstSite
            .getByTestId(/^(edit|add)$/)
            .first()
            .click();

        const panel = page.locator('dot-apps-configuration-detail');
        await panel.waitFor({ state: 'visible', timeout: 15000 });

        // Save is disabled until the whole app form is valid, so the one required
        // parameter is filled here rather than assumed to be already set.
        //
        // `input, textarea` because the control a parameter renders follows its declared
        // type, not its name: this one is a STRING and comes out as a textarea.
        const required = panel.getByTestId(REQUIRED_PARAM).locator('input, textarea').first();
        if ((await required.count()) && !(await required.inputValue())) {
            await required.fill('e2e');
        }

        return panel;
    }

    /**
     * Saves the configuration and waits for the write to land.
     *
     * Saving navigates back to the listing on success, so that navigation is the
     * signal. Without waiting for it, the next `goto` races an in-flight one and the
     * listing never renders.
     */
    async function saveConfiguration(page: import('@playwright/test').Page) {
        await page.getByTestId('saveBtn').click();
        await page.waitForURL(new RegExp(`#/apps/${APP_KEY}$`), { timeout: 20000 });
        await page.locator('dot-apps-configuration-item').first().waitFor({ timeout: 20000 });
    }

    test('add and persist custom properties @smoke', async ({ page }) => {
        const [idKey, themeKey] = [unique('analyticsId'), unique('theme')];

        const panel = await openAppConfiguration(page);
        const properties = new KeyValueField(page, panel);
        await properties.expectVisible();

        await properties.addEntry(idKey, 'UA-4419-22');
        await properties.addEntry(themeKey, 'dark');
        await properties.expectEntry(idKey, 'UA-4419-22');
        await properties.expectEntry(themeKey, 'dark');

        await saveConfiguration(page);

        const reopened = await openAppConfiguration(page);
        const reloaded = new KeyValueField(page, reopened);
        await reloaded.expectEntry(idKey, 'UA-4419-22');
        await reloaded.expectEntry(themeKey, 'dark');

        // This is the only test here that writes: put the configuration back.
        await reloaded.deleteEntryByKey(idKey);
        await reloaded.deleteEntryByKey(themeKey);
        await saveConfiguration(page);
    });

    test('the hidden indicator is visible without hovering the row @smoke', async ({ page }) => {
        // FR-018 — the indicator communicates state, so unlike the remove and
        // drag actions it must never be hover-gated. A reviewer looking at a
        // list of properties has to be able to tell which ones are secret
        // without touching the mouse.
        const tokenKey = unique('apiToken');

        const panel = await openAppConfiguration(page);
        const properties = new KeyValueField(page, panel);
        await properties.expectVisible();

        // Mark it secret in the entry row, before adding — the only place that choice exists.
        await panel.getByTestId('dot-key-value-new-visibility-toggle').click();
        await properties.addEntry(tokenKey, 'super-secret-value');

        // Matched on `title`, not text: the key cell wraps its key in branches whose
        // whitespace an anchored regex will not match, and a long key is clipped anyway.
        const row = panel
            .getByTestId('dot-key-value-key')
            .filter({ has: page.locator(`[title="${tokenKey}"]`) })
            .locator('xpath=ancestor::tr[contains(@class,"dot-key-value-table-row")][1]');

        // No hover anywhere before asserting.
        await expect(row.getByTestId('dot-key-value-hidden-icon')).toHaveText('lock');
        await expect(row.getByTestId('dot-key-value-label')).toBeVisible();
        await expect(row).not.toContainText('super-secret-value');

        // Visibility is settled at creation and never revisited: the server returns a
        // mask rather than the secret, so revealing would show the mask and saving it
        // would write it over the secret.
        await expect(row.getByTestId('dot-key-value-visibility-toggle')).toHaveCount(0);
        await expect(row.getByTestId('dot-key-value-value-output')).toHaveCount(0);

        // The remove action on the same row stays unrevealed until hovered.
        await properties.expectActionUnrevealed(tokenKey, 'dot-key-value-delete-button');
        await properties.expectActionRevealedOnHover(tokenKey, 'dot-key-value-delete-button');
    });

    test('rows can be reordered @smoke', async ({ page }) => {
        const [alpha, beta] = [unique('alpha'), unique('beta')];

        const panel = await openAppConfiguration(page);
        const properties = new KeyValueField(page, panel);
        await properties.expectVisible();

        // New pairs go to the top, so adding alpha then beta puts beta above it.
        await properties.addEntry(alpha, 'first');
        await properties.addEntry(beta, 'second');
        await properties.expectRelativeKeyOrder([beta, alpha]);

        await properties.dragRowToKey(alpha, beta);
        await properties.expectRelativeKeyOrder([alpha, beta]);
    });
});
