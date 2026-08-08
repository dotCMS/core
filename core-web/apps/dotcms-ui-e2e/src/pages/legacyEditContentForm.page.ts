import { expect, type Frame, type Page } from '@playwright/test';
import { clickAddNewContentFromList, goToContentList } from '@utils/contentListingNavigation';

const LEGACY_EDIT_FRAME_URL_PATTERN = /edit_contentlet/i;

export class LegacyEditContentFormPage {
    constructor(private page: Page) {}

    /**
     * Returns the legacy content edit iframe (edit_contentlet JSP) inside the PrimeNG dialog,
     * not the Dojo listing frame (view_contentlets).
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
     * Navigates to create new content in the legacy editor (Dojo listing → Add New Content).
     */
    async goToLegacyNew(contentTypeVariable: string) {
        await goToContentList(this.page, contentTypeVariable);
        await clickAddNewContentFromList(this.page);

        const frame = await this.getLegacyContentFrame();
        await frame.locator('dotcms-binary-field').first().waitFor({
            state: 'attached',
            timeout: 20000
        });
        await frame.getByTestId('dropzone').first().waitFor({ state: 'visible', timeout: 15000 });
    }
}
