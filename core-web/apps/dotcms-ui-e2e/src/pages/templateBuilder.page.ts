import { Locator, Page } from '@playwright/test';

/**
 * Page object for the UVE (Universal Visual Editor) Layout tab — the drawn-template
 * GridStack canvas at /edit-page/layout.
 */
export class TemplateBuilderPage {
    constructor(private page: Page) {}

    async navigateTo(pageUrl: string, params: Record<string, string> = {}) {
        const query = new URLSearchParams({ url: pageUrl, ...params }).toString();
        await this.page.goto(`/dotAdmin/#/edit-page/layout?${query}`);
    }

    getOverlay(): Locator {
        return this.page.getByTestId('template-builder-disabled-overlay');
    }

    getBox(index: number): Locator {
        return this.page.getByTestId(`builder-box-${index}`);
    }

    /**
     * Deletes the (single) container in the given box via the trash icon + confirm popup.
     * This is a plain click interaction — no GridStack drag is needed to produce a real
     * templateChange edit.
     */
    async deleteContainerFromBox(boxIndex: number): Promise<void> {
        await this.getBox(boxIndex).getByTestId('btn-trash-container').click();

        // PrimeNG p-confirmPopup default accept button. Confirmed against the installed
        // primeng@21.1.3 package (fesm2022/primeng-config.mjs: `accept: 'Yes'`) — this
        // repo does not override that translation.
        await this.page.getByRole('button', { name: 'Yes' }).click();
    }

    async goToContentTab(): Promise<void> {
        await this.page.getByRole('button', { name: 'Content' }).click();
    }
}
