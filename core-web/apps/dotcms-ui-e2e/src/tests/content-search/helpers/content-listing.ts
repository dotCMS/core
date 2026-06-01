import { type FrameLocator, type Page } from '@playwright/test';
import { getLegacyFrame } from '@utils/iframe';

/**
 * Focused helper for the Dojo-based content listing portlet.
 * Covers only the selectors needed for portlet integrity tests.
 * Dojo elements live inside iframe[name="detailFrame"] — use `frame` for all interactions.
 */
export class ContentListingHelper {
    readonly frame: FrameLocator;

    constructor(private page: Page) {
        this.frame = getLegacyFrame(page);
    }

    get resultsTable() {
        return this.frame.locator('#results_table');
    }

    get searchInput() {
        return this.frame.locator('#allFieldTB');
    }

    get listViewButton() {
        return this.frame.getByLabel('List', { exact: true });
    }

    get cardViewButton() {
        return this.frame.getByLabel('Card');
    }

    get addDropdownButton() {
        return this.frame.locator('.dijitDropDownButton [role="button"]').first();
    }

    get workflowActionsButton() {
        return this.frame.getByRole('button', { name: 'Available Workflow Actions' });
    }

    get firstRowCheckbox() {
        return this.frame.locator('#checkbox0');
    }

    get advancedFilterLink() {
        return this.frame.getByRole('link', { name: 'Advanced' });
    }

    get hideFilterLink() {
        return this.frame.getByRole('link', { name: 'Hide' });
    }

    get clearFilterButton() {
        return this.frame.getByLabel('Clear');
    }

    /**
     * Waits for the results table to be visible (iframe fully initialized).
     */
    async waitForReady() {
        await this.frame
            .locator('.dijitDropDownButton')
            .first()
            .waitFor({ state: 'visible', timeout: 20000 });
    }

    /**
     * Fills the all-fields search input and submits.
     */
    async searchFor(text: string) {
        await this.searchInput.waitFor({ state: 'visible', timeout: 10000 });
        await this.searchInput.fill(text);
        await this.page.keyboard.press('Enter');
    }

    /**
     * Clicks the "createOptions" gear button and then "Show Query".
     */
    async openQueryModal() {
        const optionsBtn = this.frame.getByRole('button', { name: 'createOptions' });
        await optionsBtn.waitFor({ state: 'visible', timeout: 10000 });
        await optionsBtn.click();

        const showQueryLink = this.frame.getByText('Show Query');
        await showQueryLink.waitFor({ state: 'visible', timeout: 5000 });
        await showQueryLink.click();
    }

    /**
     * Opens the add-content dropdown (Dojo). Use `force: true` to handle animation flicker.
     */
    async openAddContentDropdown() {
        await this.addDropdownButton.waitFor({ state: 'visible', timeout: 10000 });
        await this.addDropdownButton.click();
    }
}
