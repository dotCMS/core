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
 */
test.describe('A malformed panel link', () => {
    test('shows the not-found error and leaves the URL without the panel params @critical', async ({
        page
    }) => {
        const sidePanel = new SidePanel(page);
        const notFound = page.getByRole('dialog').filter({ hasText: 'This URL does not exist.' });

        await page.goto(`${Portlet.Content}?editContent=wrong-identifier&editContentLang=1`);

        await expect(notFound).toBeVisible({ timeout: 20000 });
        await notFound.getByRole('button', { name: 'Accept' }).click();
        await expect(notFound).toBeHidden();

        await expect(page).not.toHaveURL(/[?&](editContent|editContentLang)=/);
        await sidePanel.expectClosed();
    });
});
