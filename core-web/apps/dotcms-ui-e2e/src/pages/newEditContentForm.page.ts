import { expect, Page } from '@playwright/test';
import { getLegacyFrame } from '@utils/iframe';
import { Portlet } from '@utils/portlets';

export class NewEditContentFormPage {
    constructor(private page: Page) {}

    /**
     * True when the shell is on an Angular content route (edit/new), not the Dojo listing.
     */
    private isOnAngularContentRoute(): boolean {
        const url = this.page.url();

        return /#\/content\//.test(url) && !/#\/c\/content/.test(url);
    }

    /** Waits for the Dojo content listing iframe and its widgets to be ready. */
    private async waitForContentListingReady() {
        const frame = getLegacyFrame(this.page);

        await frame
            .locator('.dijitDropDownButton')
            .first()
            .waitFor({ state: 'visible', timeout: 20000 });

        await frame
            .locator('dot-data-view-button.hydrated')
            .waitFor({ state: 'visible', timeout: 20000 });
    }

    /**
     * Navigates to the Content portlet filtered by content type.
     * URL: /dotAdmin/#/c/content?filter=ContentTypeName
     */
    async goToContentList(contentTypeVariable: string) {
        const listingUrl = `${Portlet.Content}?filter=${contentTypeVariable}`;

        // Hash-only navigation from Angular edit routes does not re-init the Dojo iframe.
        if (this.isOnAngularContentRoute()) {
            await this.page.goto('/dotAdmin/');
            await this.page.waitForLoadState('domcontentloaded');
        }

        await this.page.goto(listingUrl);
        await this.page.waitForLoadState('domcontentloaded');
        await this.waitForContentListingReady();
    }

    /**
     * From the content listing (Dojo portlet inside iframe), clicks the "+"
     * dropdown and selects "Add New Content" to open the new content form.
     */
    async clickNewContentFromList() {
        const frame = getLegacyFrame(this.page);

        const addButton = frame.locator('.dijitDropDownButton [role="button"]').first();
        const addNewOption = frame.getByRole('menuitem', { name: 'Add New Content' });

        await addButton.waitFor({ state: 'visible', timeout: 10000 });

        // Open the dropdown and select the item atomically. The Dojo menu auto-closes
        // and the listing portlet can re-render after a hash navigation, so retry the
        // whole open+click rather than just the visibility check.
        await expect(async () => {
            await addButton.click();
            await expect(addNewOption).toBeVisible({ timeout: 2000 });
            await addNewOption.click({ timeout: 2000 });
        }).toPass({ timeout: 20000 });

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
     * Clicks the workflow action that persists the content (Save or Publish) and waits for the API response.
     * Prefer "Save" when both Save and Publish are visible — the new command bar can show multiple
     * workflow buttons at once, so a single regex locator would match 2+ roles and break strict mode.
     */
    async save() {
        const saveButton = this.page.getByRole('button', { name: 'Save' });
        const publishButton = this.page.getByRole('button', { name: 'Publish' });
        const actionButton = (await saveButton.isVisible()) ? saveButton : publishButton;

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
     * Navigates directly to edit an existing contentlet by its inode.
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

    /**
     * Navigates to create new content with a folderPath query param.
     * Uses the Angular content route (/content/new/), not the legacy
     * Dojo iframe route (/c/content/new/).
     */
    async goToNewWithFolderPath(contentType: string, folderPath: string) {
        await this.page.goto(`/dotAdmin/#/content/new/${contentType}?folderPath=${folderPath}`);
        await this.page.waitForLoadState('domcontentloaded');
        await this.page.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });
    }

    /**
     * Navigates to create a new file-asset content with a folderPath query param.
     * File asset types don't expose data-testid="title", so this waits for the
     * built-in hostFolder field instead.
     */
    async goToNewFileAssetWithFolderPath(contentType: string, folderPath: string) {
        await this.page.goto(`/dotAdmin/#/content/new/${contentType}?folderPath=${folderPath}`);
        await this.page.waitForLoadState('domcontentloaded');
        await this.page
            .getByTestId('field-hostFolder')
            .waitFor({ state: 'visible', timeout: 15000 });
    }
}
