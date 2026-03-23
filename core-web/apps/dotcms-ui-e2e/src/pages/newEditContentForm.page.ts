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

        // Click the Dojo "+" dropdown button (use role instead of fragile ID)
        const addButton = frame.locator('.dijitDropDownButton [role="button"]').first();
        await addButton.waitFor({ state: 'visible', timeout: 15000 });
        await addButton.click();

        // The dropdown menu renders inside the iframe.
        // Use force:true because Dojo menus can flicker during animation.
        const addNewOption = frame.locator('.dijitMenuItemLabel', { hasText: 'Add New Content' });
        await addNewOption.waitFor({ state: 'visible', timeout: 10000 });
        await addNewOption.click({ force: true });

        await this.page.waitForLoadState('networkidle');
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

    async save() {
        const saveButtonLocator = this.page.getByRole('button', {
            name: 'Save'
        });
        await expect(saveButtonLocator).toBeVisible();

        const responsePromise = this.page.waitForResponse((response) => {
            return (
                response.status() === 200 && response.url().includes('/api/v1/workflow/actions/')
            );
        });
        await saveButtonLocator.click();
        await responsePromise;
    }

    /**
     * Navigates to edit an existing contentlet.
     * Goes through the Content portlet first to initialize the Dojo app,
     * then navigates to the specific content.
     */
    async goToContent(id: string) {
        await this.page.goto(Portlet.Content);
        await this.page.waitForLoadState('networkidle');
        await this.page.goto(`/dotAdmin/#/content/${id}`);
        await this.page.waitForLoadState('networkidle');
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
