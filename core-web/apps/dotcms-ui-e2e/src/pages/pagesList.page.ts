import { Locator, Page, expect } from '@playwright/test';

export class PagesListPage {
    constructor(private page: Page) {}

    /**
     * Navigate to the pages list page
     */
    async navigateTo() {
        await this.page.goto('/dotAdmin/#/pages');
    }

    getPageListItems() {
        return this.page.getByTestId('pages-listing-panel').locator('tbody').getByRole('row');
    }

    getRowByTitle(title: string) {
        const rows = this.getPageListItems();
        return rows.filter({ hasText: title });
    }

    async activeArchivedFilter() {
        await this.page.getByTestId('pages-listing-panel').getByText('Show Archived').click();
    }

    async doActionOnPage(rowLocator: Locator, action: 'Unpublish' | 'Archive' | 'Destroy') {
        await rowLocator.getByRole('button').click();

        const actionLocator = this.page.locator('.p-tieredmenu').getByLabel(action).locator('a');

        await expect(actionLocator).toBeVisible();

        // Wait for the workflow action itself. Any `/api/content/_search` response would also
        // match, including ones that were already in flight before the click and that still
        // carry the page's previous status.
        const responsePromise = this.page.waitForResponse((response) => {
            return (
                response.request().method() === 'PUT' &&
                response.status() === 200 &&
                /\/api\/v1\/workflow\/actions\/[^/]+\/fire/.test(response.url())
            );
        });
        await actionLocator.click();
        await responsePromise;
    }

    getStatusChip(rowLocator: Locator) {
        return rowLocator.locator('dot-contentlet-status-badge');
    }

    getStatusChipText(rowLocator: Locator) {
        return this.getStatusChip(rowLocator).locator('p-tag').textContent();
    }

    /**
     * Wait until the row's status chip shows the given status. The list re-reads the row after
     * the workflow action returns, so the chip changes a moment after the response, not with it.
     */
    async expectStatusChip(rowLocator: Locator, status: string) {
        await expect(this.getStatusChip(rowLocator).locator('p-tag')).toContainText(status);
    }
}
