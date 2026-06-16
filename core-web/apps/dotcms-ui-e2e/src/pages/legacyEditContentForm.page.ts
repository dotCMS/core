import { expect, type Frame, type Page } from '@playwright/test';

const LEGACY_EDIT_FRAME_URL_PATTERN = /edit_contentlet|portlet\/ext\/contentlet/;

export class LegacyEditContentFormPage {
    constructor(private page: Page) {}

    /**
     * Returns the legacy content edit iframe (edit_contentlet JSP), distinct from the shell detailFrame.
     */
    async getLegacyContentFrame(): Promise<Frame> {
        let frame: Frame | undefined;

        await expect
            .poll(
                () => {
                    frame = this.page
                        .frames()
                        .find((f) => LEGACY_EDIT_FRAME_URL_PATTERN.test(f.url()));
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
     * Navigates to create new content in the legacy editor (Dojo portlet inside detailFrame).
     * URL: /dotAdmin/#/c/content/new/{contentTypeVariable}
     */
    async goToLegacyNew(contentTypeVariable: string) {
        await this.page.goto(`/dotAdmin/#/c/content/new/${contentTypeVariable}`);
        await this.page.waitForLoadState('domcontentloaded');

        const frame = await this.getLegacyContentFrame();
        await frame.locator('dotcms-binary-field').first().waitFor({
            state: 'attached',
            timeout: 20000
        });
        await frame.getByTestId('dropzone').first().waitFor({ state: 'visible', timeout: 15000 });
    }
}
