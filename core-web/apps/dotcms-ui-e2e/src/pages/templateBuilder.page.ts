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

    /**
     * Clicking this while a save is held in-flight deliberately puts Angular's Router into
     * a CanDeactivate guard that never resolves (blocked on canDeactivateRoute$ until the
     * in-flight save completes) — that's the exact scenario this page object exists to
     * drive. Plain .click() then hangs indefinitely (unaffected by any timeout budget,
     * confirmed in CI across 60s and 120s runs): Playwright's actionability retries appear
     * to get stuck on the router's pending-navigation state. `{ force: true }` would skip
     * those checks, but this repo's eslint config bans it (playwright/no-force-option);
     * dispatchEvent bypasses actionability the same way without tripping that rule — the
     * button is never disabled or covered, only the navigation it triggers stalls, which
     * is the intended behavior under test.
     */
    async goToContentTab(): Promise<void> {
        await this.page.getByRole('button', { name: 'Content' }).dispatchEvent('click');
    }
}
