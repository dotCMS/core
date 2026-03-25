import { expect, Page } from '@playwright/test';

import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';

export class NewEditContentFormPage {
    constructor(private page: Page) {}

    /**
     * Navigates to the Content portlet filtered by content type.
     * URL: /dotAdmin/#/c/content?filter=ContentTypeName
     */
    async goToContentList(contentTypeVariable: string) {
        await this.page.goto(`${Portlet.Content}?filter=${contentTypeVariable}`);
        await this.page.waitForLoadState('networkidle');
    }

    /**
     * From the content listing (Dojo portlet inside iframe), clicks the "+"
     * dropdown and selects "Add New Content" to open the new content form.
     */
    async clickNewContentFromList() {
        const frame = getLegacyFrame(this.page);

        // Wait for the Dojo iframe to fully load and widgets to initialize
        await frame
            .locator('.dijitDropDownButton')
            .first()
            .waitFor({ state: 'visible', timeout: 15000 });
        // Small delay for Dojo widget initialization after DOM is visible
        await this.page.waitForTimeout(500);

        // Click the Dojo "+" dropdown button
        const addButton = frame.locator('.dijitDropDownButton [role="button"]').first();
        await addButton.click();

        // The dropdown menu renders inside the iframe.
        // Use force:true because Dojo menus can flicker during animation.
        const addNewOption = frame.locator('.dijitMenuItemLabel', { hasText: 'Add New Content' });
        await addNewOption.waitFor({ state: 'visible', timeout: 10000 });
        await addNewOption.click({ force: true });

        // Wait for the Angular form to render (replaces networkidle which is unreliable in SPAs)
        await this.page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });
    }

    async fillTextField(text: string) {
        const textFieldLocator = this.page.getByTestId('title');
        await textFieldLocator.waitFor({ state: 'visible', timeout: 10000 });
        await textFieldLocator.fill(text);
    }

    get siteOrFolderFieldLocator() {
        return this.page.getByTestId('field-siteOrFolderField');
    }

    async selectSiteOrFolderField() {
        const siteOrFolderFieldLocator = this.siteOrFolderFieldLocator;
        await siteOrFolderFieldLocator.click();

        const treeNodeLocator = this.page.locator('.p-treenode');
        const textContent = await treeNodeLocator.first().textContent();
        await treeNodeLocator.first().click();

        const labelLocator = this.page.locator('.p-treeselect-label');
        await expect(labelLocator).toHaveText(`//${textContent}`);
        return textContent;
    }

    /**
     * Clicks the primary workflow action button (Save or Publish) and waits for the API response.
     * New content shows "Save", existing content shows "Publish".
     */
    async save() {
        // Match either Save or Publish — the primary action button
        const actionButton = this.page.getByRole('button', { name: /^(Save|Publish)$/ });
        await expect(actionButton).toBeVisible();

        const responsePromise = this.page.waitForResponse((response) => {
            return (
                response.status() === 200 && response.url().includes('/api/v1/workflow/actions/')
            );
        });
        await actionButton.click();
        await responsePromise;
    }

    /**
     * Navigates to edit an existing contentlet.
     * Goes through the Content portlet first to initialize the Dojo app,
     * then navigates to the specific content.
     */
    async goToContent(id: string) {
        await this.page.goto(`/dotAdmin/#/content/${id}`);
        await this.page.waitForLoadState('domcontentloaded');
        // Wait for the Angular content form to render
        await this.page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });
    }

    /**
     * Navigates to create new content of the given type.
     * Goes to the Content portlet filtered by content type (Dojo listing),
     * then clicks the "+" dropdown → "Add New Content".
     */
    async goToNew(contentType: string) {
        await this.goToContentList(contentType);
        await this.clickNewContentFromList();
    }
}
