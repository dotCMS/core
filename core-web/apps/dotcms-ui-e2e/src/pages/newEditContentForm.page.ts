import { expect, type Locator, Page, type Request } from '@playwright/test';
import { type Contentlet } from '@requests/contentlets';
import { clickAddNewContentFromList, goToContentList } from '@utils/contentListingNavigation';

/**
 * Whether a request fires a workflow action: what Save and Publish send, as
 * `PUT /api/v1/workflow/actions/{actionId}/fire` (`DotWorkflowActionsFireService.fireTo`) or
 * `PUT /api/v1/workflow/actions/default/fire/{action}`.
 */
function isWorkflowFire(request: Request): boolean {
    const { pathname } = new URL(request.url());

    return (
        request.method() === 'PUT' &&
        pathname.startsWith('/api/v1/workflow/actions/') &&
        pathname.includes('/fire')
    );
}

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
     * Clicks the workflow action that persists the content (Save or Publish) and returns the
     * contentlet it saved, read from the workflow fire response.
     * Prefer "Save" when both Save and Publish are visible — the new command bar can show multiple
     * workflow buttons at once, so a single regex locator would match 2+ roles and break strict mode.
     *
     * The response is the only place a new contentlet's id shows up: content created from the
     * listing is edited in the side panel over it (#38303), and the address never changes.
     */
    async save(): Promise<Contentlet> {
        const saveButton = this.root.getByRole('button', { name: 'Save' });
        const publishButton = this.root.getByRole('button', { name: 'Publish' });
        const actionButton = (await saveButton.isVisible()) ? saveButton : publishButton;

        await expect(actionButton).toBeVisible();

        const responsePromise = this.page.waitForResponse(
            (response) => response.status() === 200 && isWorkflowFire(response.request())
        );
        await actionButton.click();
        const { entity } = await (await responsePromise).json();

        expect(entity?.inode, 'the workflow fire response carries no contentlet').toBeTruthy();

        return entity as Contentlet;
    }

    /**
     * Records every workflow fire the page sends from now on, for a test that has to prove a save
     * was refused before it reached the server. The returned array fills as requests go out.
     */
    recordWorkflowFires(): string[] {
        const fires: string[] = [];

        this.page.on('request', (request) => {
            if (isWorkflowFire(request)) {
                fires.push(request.url());
            }
        });

        return fires;
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
     * Opens saved content afresh on the full-screen route, with a full page load, to read back
     * what the save stored.
     *
     * {@link goToContent} alone is not enough after a save from the side panel: changing the hash
     * keeps the app, and with it the panel the content was created in, which belongs to the admin
     * root rather than the route. Two forms would then match every field locator.
     */
    async reopen(inode: string) {
        await this.page.goto(`/dotAdmin/#/content/${inode}`);
        await this.page.reload();
        await this.page.waitForLoadState('domcontentloaded');
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
