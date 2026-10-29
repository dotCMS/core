import { expect, type Locator, Page } from '@playwright/test';
import { clickAddNewContentFromList, goToContentList } from '@utils/contentListingNavigation';

export class NewEditContentFormPage {
    /**
     * @param page - Playwright page
     * @param root - Where the form is. Defaults to the whole page, which is enough while one form
     *   is open; pass a side panel (`getByTestId('side-panel')`) when another one is open behind
     *   it, since both carry the same field test ids and workflow buttons (#38303).
     */
    constructor(
        private page: Page,
        private root: Page | Locator = page
    ) {}

    /**
     * Navigates to the Content portlet filtered by content type.
     * URL: /dotAdmin/#/c/content?filter=ContentTypeName
     */
    async goToContentList(contentTypeVariable: string) {
        await goToContentList(this.page, contentTypeVariable);
    }

    /**
     * From the content listing (Dojo portlet inside iframe), clicks the "+"
     * dropdown and selects "Add New Content", which opens the new content form in the side panel
     * over the listing (#38303).
     */
    async clickNewContentFromList() {
        await clickAddNewContentFromList(this.page);

        // Wait for the Angular form to render (replaces networkidle which is unreliable in SPAs)
        await this.root.getByTestId('title').waitFor({ state: 'visible', timeout: 15000 });
    }

    async fillTextField(text: string) {
        const textFieldLocator = this.root.getByTestId('title');
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
        const saveButton = this.root.getByRole('button', { name: 'Save' });
        const publishButton = this.root.getByRole('button', { name: 'Publish' });
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
