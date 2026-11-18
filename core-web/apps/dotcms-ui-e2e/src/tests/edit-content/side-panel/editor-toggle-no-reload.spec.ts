import { iFramesLocators } from '@locators/globalLocators';
import { expect, type Page, test } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import {
    type ContentType,
    createFakeContentType,
    deleteContentType,
    setNewContentEditor
} from '@requests/contentType';
import { goToContentList } from '@utils/contentListingNavigation';
import { getLegacyFrame } from '@utils/iframe';
import { uniqueSuffix } from '@utils/utils';

import { SidePanel } from './helpers/side-panel';

/**
 * #38303, T176: turning a content type's "new edit content screen" on or off takes effect on the
 * next open of its content in the side panel, without reloading the browser. The editor choice is
 * read fresh on every open; it used to be cached for the life of the app.
 */

/** A flag set on the window: it is gone if the page reloads. */
const NO_RELOAD_MARK = '__e2eEditorToggleNoReload';

test.describe('A toggled "new edit content screen" takes effect without a reload', () => {
    let contentType: ContentType | null = null;
    let title: string;

    test.beforeEach(async ({ request }) => {
        const suffix = uniqueSuffix();
        // createFakeContentType gives it a Title text field and the new editor.
        contentType = await createFakeContentType(request, {
            name: `E2ESidePanelToggle${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: true }
        });

        title = `Toggle content ${suffix}`;
        await createContentlet(request, { contentType: contentType.variable, title });
    });

    test.afterEach(async ({ request }) => {
        // Deleting the content type deletes its contentlet.
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    /**
     * Opens the content from the Content Search listing's row, as a click on its title does.
     *
     * The listing's `editContentlet` (`menu_actions_js.jsp`) drops a click that comes within a
     * second of the one before, while its `clickTimeOut` is set; this test opens faster than a
     * person, so it waits for that to clear first.
     */
    const openFromListing = async (page: Page, sidePanel: SidePanel): Promise<void> => {
        const resultsTable = getLegacyFrame(page).locator('#results_table');
        await expect(resultsTable).toBeVisible({ timeout: 20000 });
        await expect
            .poll(() =>
                resultsTable.evaluate(
                    () => !(window as unknown as { clickTimeOut?: unknown }).clickTimeOut
                )
            )
            .toBe(true);

        await resultsTable.locator('tr').nth(1).getByRole('link').first().click();
        await sidePanel.expectOpen();
    };

    const expectNewEditor = async (sidePanel: SidePanel): Promise<void> => {
        await expect(sidePanel.front.getByTestId('title')).toHaveValue(title, { timeout: 20000 });
        await expect(sidePanel.front.locator(iFramesLocators.legacy_editor_iframe)).toHaveCount(0);
    };

    const expectLegacyEditor = async (sidePanel: SidePanel): Promise<void> => {
        await expect(sidePanel.front.locator(iFramesLocators.legacy_editor_iframe)).toBeVisible({
            timeout: 20000
        });
        await expect(sidePanel.front.getByTestId('title')).toHaveCount(0);
    };

    /** Closes the panel and waits for the listing's reload that the close starts. */
    const closePanel = async (page: Page, sidePanel: SidePanel): Promise<void> => {
        const listingReloaded = page.waitForResponse((response) =>
            response.url().includes('ContentletAjax.searchContentlets.dwr')
        );

        await sidePanel.close();
        await sidePanel.expectClosed();
        await listingReloaded;
    };

    test('opens the same content in the editor the flag names, each time it is toggled @critical', async ({
        page,
        request
    }) => {
        const sidePanel = new SidePanel(page);
        const typeId = (contentType as ContentType).id;

        await goToContentList(page, (contentType as ContentType).variable);
        await page.evaluate((mark) => {
            (window as unknown as Record<string, boolean>)[mark] = true;
        }, NO_RELOAD_MARK);

        await openFromListing(page, sidePanel);
        await expectNewEditor(sidePanel);
        await closePanel(page, sidePanel);

        await setNewContentEditor(request, typeId, false);
        await openFromListing(page, sidePanel);
        await expectLegacyEditor(sidePanel);
        await closePanel(page, sidePanel);

        await setNewContentEditor(request, typeId, true);
        await openFromListing(page, sidePanel);
        await expectNewEditor(sidePanel);

        const notReloaded = await page.evaluate(
            (mark) => (window as unknown as Record<string, boolean>)[mark] === true,
            NO_RELOAD_MARK
        );
        expect(notReloaded, 'the page reloaded between opens').toBe(true);
    });
});
