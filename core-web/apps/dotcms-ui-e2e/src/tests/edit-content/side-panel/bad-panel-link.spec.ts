import { expect, test } from '@playwright/test';
import { Portlet } from '@utils/portlets';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, T172: a panel link the author cannot have meant, a value of the wrong shape, tells them
 * with the error a link to missing content shows, and loses its params; nothing opens. Before, the
 * reader took the value as absent: no dialog, no panel, and the junk stayed in the address bar.
 *
 * On the legacy content portlet (`/c/content`), one of the five surfaces that read the panel URL;
 * they all refuse through the same `DotPanelUrlStateService.refuse`.
 *
 * T175: the portlet refuses on init, which on a fresh load (a new build, so the messages are
 * fetched again) is before the app's messages have arrived, and the dialog showed its raw keys. The
 * test loads the app that way: no messages kept from the login, and their request held back.
 */
test.describe('A malformed panel link', () => {
    test.beforeEach(async ({ page }) => {
        await page.addInitScript(() => localStorage.removeItem('dotMessagesKeys'));
        await page.route('**/api/v2/languages/*/keys', async (route) => {
            await new Promise((resolve) => setTimeout(resolve, 2000));
            await route.continue();
        });
    });

    test('shows the not-found error and leaves the URL without the panel params @critical', async ({
        page
    }) => {
        const sidePanel = new SidePanel(page);
        const notFound = page.getByRole('dialog');

        await page.goto(`${Portlet.Content}?editContent=wrong-identifier&editContentLang=1`);

        await expect(notFound).toBeVisible({ timeout: 20000 });
        await expect(notFound).toContainText('This URL does not exist.');
        await expect(notFound).not.toContainText('dot.common');
        await notFound.getByRole('button', { name: 'Accept' }).click();
        await expect(notFound).toBeHidden();

        await expect(page).not.toHaveURL(/[?&](editContent|editContentLang)=/);
        await sidePanel.expectClosed();
    });
});
