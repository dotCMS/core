import { expect, type Frame, type Page } from '@playwright/test';
import { clickAddNewContentFromList, goToContentList } from '@utils/contentListingNavigation';
import { getLegacyFrame } from '@utils/iframe';

const LEGACY_EDIT_FRAME_URL_PATTERN = /edit_contentlet/i;

export class LegacyEditContentFormPage {
    constructor(private page: Page) {}

    /** The side panel the listing opens the legacy editor in (#38303). */
    get sidePanel() {
        return this.page.getByTestId('side-panel');
    }

    /**
     * Returns the legacy content edit iframe (edit_contentlet JSP) inside the side panel, not the
     * Dojo listing frame (view_contentlets).
     *
     * Found by URL rather than through the panel's `legacy-screen-iframe`, so it also resolves the
     * editor on the full-screen route and hands back a `Frame` the field helpers already take.
     */
    async getLegacyContentFrame(): Promise<Frame> {
        let frame: Frame | undefined;

        await expect
            .poll(
                () => {
                    const matchingFrames = this.page
                        .frames()
                        .filter((f) => LEGACY_EDIT_FRAME_URL_PATTERN.test(f.url()));

                    frame = matchingFrames.at(-1);
                    return frame;
                },
                { timeout: 20000 }
            )
            .toBeTruthy();

        if (!frame) {
            throw new Error('Legacy content edit iframe (edit_contentlet) not found');
        }

        return frame;
    }

    /**
     * Navigates to create new content in the legacy editor (Dojo listing → Add New Content), which
     * opens it in the side panel over the listing.
     */
    async goToLegacyNew(contentTypeVariable: string) {
        await goToContentList(this.page, contentTypeVariable);
        await clickAddNewContentFromList(this.page);
        await expect(this.sidePanel).toBeVisible({ timeout: 20000 });

        const frame = await this.getLegacyContentFrame();
        await frame.locator('dotcms-binary-field').first().waitFor({
            state: 'attached',
            timeout: 20000
        });
        await frame.getByTestId('dropzone').first().waitFor({ state: 'visible', timeout: 15000 });
    }

    /**
     * Opens an existing contentlet in the legacy editor (Dojo listing -> first result row), which
     * opens it in the side panel over the listing.
     *
     * Waits only for the edit_contentlet iframe, not for any field inside it, so that a field
     * failing to hydrate surfaces as an assertion in the test rather than a navigation timeout
     * here.
     */
    async goToLegacyEdit(contentTypeVariable: string) {
        await goToContentList(this.page, contentTypeVariable);

        const resultsTable = getLegacyFrame(this.page).locator('#results_table');
        await expect(resultsTable).toBeVisible({ timeout: 20000 });
        await resultsTable.locator('tr').nth(1).getByRole('link').first().click();
        await expect(this.sidePanel).toBeVisible({ timeout: 20000 });

        await this.getLegacyContentFrame();
    }
}
