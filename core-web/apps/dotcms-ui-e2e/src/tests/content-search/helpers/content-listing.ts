import { type FrameLocator, type Page } from '@playwright/test';
import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';

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
        return this.frame.locator('dot-data-view-button').getByLabel('List', { exact: true });
    }

    get cardViewButton() {
        return this.frame.locator('dot-data-view-button').getByLabel('Card');
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
     * Navigates to the content portlet and waits for the Dojo iframe to be ready.
     */
    async goto() {
        await this.page.goto(Portlet.Content);
        await this.waitForReady();
    }

    /**
     * Waits for the results table to be visible (iframe fully initialized).
     */
    async waitForReady() {
        await this.frame
            .locator('.dijitDropDownButton')
            .first()
            .waitFor({ state: 'visible', timeout: 20000 });

        await this.frame
            .locator('dot-data-view-button.hydrated')
            .waitFor({ state: 'visible', timeout: 20000 });

        await this.listViewButton.waitFor({ state: 'visible', timeout: 10000 });
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
     * Waits for #queryResults to confirm the modal fully rendered.
     *
     * The "Show Query" menu item lives in a Dojo popup that is always in the DOM
     * but hidden until the button is clicked. In CI the popup can take longer to
     * transition to visible, so we allow up to 15 s before giving up.
     */
    async openQueryModal() {
        const optionsBtn = this.frame.getByRole('button', { name: 'createOptions' });
        await optionsBtn.waitFor({ state: 'visible', timeout: 10000 });
        await optionsBtn.click();

        const showQueryLink = this.frame.getByText('Show Query');
        await showQueryLink.waitFor({ state: 'visible', timeout: 15000 });
        await showQueryLink.click();

        await this.frame.locator('#queryResults').waitFor({ state: 'visible', timeout: 15000 });
    }

    /**
     * Opens the add-content dropdown (Dojo).
     */
    async openAddContentDropdown() {
        await this.addDropdownButton.waitFor({ state: 'visible', timeout: 10000 });
        await this.addDropdownButton.click();
    }

    /**
     * Returns the "API" span element inside the query modal.
     * The element is a <span class="dot-api-link"> — not an <a> tag — and calls
     * window.open() via an async AJAX POST when clicked.
     */
    get queryModalApiLink() {
        return this.frame.locator('.dot-api-link');
    }
}
