import { expect, type Page } from '@playwright/test';
import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';

/** True when the shell is on an Angular content route (edit/new), not the Dojo listing. */
export function isOnAngularContentRoute(page: Page): boolean {
    const url = page.url();

    return /#\/content\//.test(url) && !/#\/c\/content/.test(url);
}

/** Waits for the Dojo content listing iframe and its widgets to be ready. */
export async function waitForContentListingReady(page: Page) {
    const frame = getLegacyFrame(page);

    await frame
        .locator('.dijitDropDownButton')
        .first()
        .waitFor({ state: 'visible', timeout: 20000 });

    await frame
        .locator('dot-data-view-button.hydrated')
        .waitFor({ state: 'visible', timeout: 20000 });
}

/**
 * Navigates to the Content portlet filtered by content type.
 * URL: /dotAdmin/#/c/content?filter={contentTypeVariable}
 */
export async function goToContentList(page: Page, contentTypeVariable: string) {
    const listingUrl = `${Portlet.Content}?filter=${contentTypeVariable}`;

    // Hash-only navigation from Angular edit routes does not re-init the Dojo iframe.
    if (isOnAngularContentRoute(page)) {
        await page.goto('/dotAdmin/');
        await page.waitForLoadState('domcontentloaded');
    }

    await page.goto(listingUrl);
    await page.waitForLoadState('domcontentloaded');
    await waitForContentListingReady(page);
}

/**
 * From the content listing (Dojo portlet inside detailFrame), opens the "+"
 * dropdown and selects "Add New Content".
 */
export async function clickAddNewContentFromList(page: Page) {
    const frame = getLegacyFrame(page);

    const addButton = frame.locator('.dijitDropDownButton [role="button"]').first();
    const addNewOption = frame.getByRole('menuitem', { name: 'Add New Content' });

    await addButton.waitFor({ state: 'visible', timeout: 10000 });

    // Open the dropdown and select the item atomically. The Dojo menu auto-closes
    // and the listing portlet can re-render after a hash navigation, so retry the
    // whole open+click rather than just the visibility check.
    await expect(async () => {
        await addButton.click();
        await expect(addNewOption).toBeVisible({ timeout: 2000 });
        await addNewOption.click({ timeout: 2000 });
    }).toPass({ timeout: 20000 });
}
