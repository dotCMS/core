import { type Page } from '@playwright/test';

/**
 * Loads the app as on a fresh load after a new build: no messages kept in local storage from the
 * login, and their request held back two seconds. Whatever builds text before they arrive shows
 * raw keys (#38303, T175, T177). Call before the page's first navigation.
 *
 * @param page The page to load that way.
 */
export async function loadMessagesLate(page: Page): Promise<void> {
    await page.addInitScript(() => localStorage.removeItem('dotMessagesKeys'));
    await page.route('**/api/v2/languages/*/keys', async (route) => {
        await new Promise((resolve) => setTimeout(resolve, 2000));
        await route.continue();
    });
}
