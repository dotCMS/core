import { expect, type Locator, type Page } from '@playwright/test';
import { Portlet } from '@utils/portlets';

/**
 * Page object for the Content Drive portlet shell.
 */
export class ContentDrivePage {
    readonly toolbar: Locator;
    readonly treeSelector: Locator;
    readonly sidebar: Locator;
    readonly currentSiteHostname: Locator;
    readonly listTitles: Locator;
    readonly treeNodeLabels: Locator;
    readonly searchField: Locator;
    readonly uploadIndicator: Locator;
    readonly uploadProgress: Locator;
    readonly toasts: Locator;

    constructor(private page: Page) {
        this.toolbar = page.getByTestId('toolbar');
        this.treeSelector = page.getByTestId('tree-selector');
        this.sidebar = page.getByTestId('sidebar');
        // The text field itself, not its `dot-content-drive-search-input` host: the shortcut under
        // test focuses the input, and only the input can be asserted focused.
        this.searchField = page.getByTestId('search-input-field');
        // The site is named by the tree's own root row rather than a header above it, so the
        // hostname is that row's label, and that row is the first one.
        this.currentSiteHostname = this.sidebar.getByTestId('tree-node-label').first();
        this.listTitles = page.getByTestId('item-title-text');
        this.treeNodeLabels = this.sidebar.getByTestId('tree-node-label');
        // The toolbar's in-flight indicator, and the position it shows when a run reports one.
        this.uploadIndicator = page.getByTestId('action-execution-indicator');
        this.uploadProgress = page.getByTestId('action-execution-progress');
        this.toasts = page.locator('.p-toast-message');
    }

    /**
     * Navigates to Content Drive and waits for the sidebar tree to render nodes.
     */
    async goTo() {
        const folderSearch = this.page
            .waitForResponse(
                (r) => r.url().includes('/api/v1/folder/search') && r.status() === 200,
                { timeout: 30000 }
            )
            .catch(() => null);

        await this.page.goto(Portlet.ContentDrive);
        await this.page.waitForLoadState('domcontentloaded');
        await expect(this.toolbar).toBeVisible({ timeout: 20000 });
        await expect(this.treeSelector).toBeVisible({ timeout: 20000 });
        await folderSearch;
        // Wait for tree nodes via projected labels (more specific than the p-tree host test id).
        await expect(this.treeNodeLabels.first()).toBeVisible({ timeout: 20000 });
        // Waited on after the tree, not before it: the hostname is the root node's label, so it only
        // exists once the folder tree has rendered.
        await expect(this.currentSiteHostname).toBeVisible({ timeout: 20000 });
    }

    async expectSiteHostname(hostname: string) {
        await expect(this.currentSiteHostname).toContainText(hostname, {
            timeout: 15000,
            ignoreCase: true
        });
    }

    /**
     * Clicks the tree toggler, which lives in the toolbar in both states. The sidebar used to carry
     * a second copy for the expanded state; it no longer does.
     */
    async toggleTree() {
        await this.toolbar.getByTestId('tree-toggler').click();
    }

    async expectTreeExpanded() {
        await expect
            .poll(async () => (await this.treeSelector.boundingBox())?.width ?? 0, {
                timeout: 10000
            })
            .toBeGreaterThan(100);
        await expect(this.treeNodeLabels.first()).toBeVisible({ timeout: 10000 });
    }

    async expectTreeCollapsed() {
        await expect
            .poll(async () => (await this.treeSelector.boundingBox())?.width ?? 0, {
                timeout: 10000
            })
            .toBeLessThan(10);
    }

    /**
     * Sorts the listing by its first sortable column and waits for the results it re-requests.
     *
     * Which column is immaterial — what matters is that a sort happened, because the table clears
     * its own keyboard anchor as part of one, and that is the state `expectSingleTabStop()` checks
     * afterwards. Picked by position rather than by header text so the assertion does not depend on
     * a translated label.
     *
     * The listing is lazy, so this is a round trip rather than a client-side reorder.
     */
    async sortByFirstColumn() {
        const search = this.page
            .waitForResponse(
                (r) => r.url().includes('/api/v1/drive/search') && r.status() === 200,
                {
                    timeout: 20000
                }
            )
            .catch(() => null);

        await this.page.getByTestId('header-column-sortable').first().click();
        await search;
    }

    async expectListContainsTitle(title: string) {
        await expect(this.listTitles.filter({ hasText: title }).first()).toBeVisible({
            timeout: 20000
        });
    }

    /** Navigates into a folder by clicking its row in the tree. */
    async openFolder(name: string) {
        await this.treeNodeLabels.filter({ hasText: name }).first().click();
        await expect(this.page.getByTestId('dropzone')).toBeVisible({ timeout: 20000 });
    }

    /**
     * Chooses several files through the real file chooser.
     *
     * The only honest proof that `multiple` is on the input: `setInputFiles` with more than one
     * path throws on an input without it, so this fails as a browser error rather than as a
     * missing row.
     */
    async chooseFilesForUpload(names: string[], baseType = 'DOTASSET') {
        const chooser = this.page.waitForEvent('filechooser');

        await this.toolbar.getByTestId('upload-button').click();

        // A folder that pins a default base type skips the selector and opens the chooser straight
        // away, so the option is clicked only when it actually appears.
        const option = this.page.getByTestId(`upload-selector-option-${baseType}`);
        if (await option.isVisible().catch(() => false)) {
            await option.click();
        }

        await (await chooser).setFiles(names.map((name) => filePayload(name)));
    }

    /**
     * Drops several files onto the listing.
     *
     * Synthesises the `DataTransfer` inside the page: this exercises the same handler a real drag
     * reaches, but it is not a drag from the desktop, which no browser automation can perform.
     */
    async dropFilesOnList(names: string[]) {
        const dropzone = this.page.getByTestId('dropzone');

        await dropzone.dispatchEvent('dragenter');
        await dropzone.dispatchEvent('drop', {
            dataTransfer: await this.page.evaluateHandle((fileNames) => {
                const transfer = new DataTransfer();
                fileNames.forEach((name) =>
                    transfer.items.add(new File([new Uint8Array(4)], name, { type: 'image/png' }))
                );

                return transfer;
            }, names)
        });
    }

    /**
     * Chooses `count` generated files, for the batch sizes a fixture file cannot express.
     *
     * The refusal for too many files starts above `CONTENT_BULK_UPLOAD_MAX_FILES`, which defaults
     * to 100, so proving it means actually choosing 101 of them.
     */
    async chooseGeneratedFilesForUpload(count: number, prefix: string) {
        await this.chooseFilesForUpload(
            Array.from({ length: count }, (_, index) => `${prefix}-${index}.png`)
        );
    }

    /**
     * The handle has arrived: the author has been told the batch is theirs to leave, and the
     * indicator is still reporting it.
     *
     * Both halves matter and they fail for different reasons. Without the message the author never
     * learns the rules changed; without the indicator the batch looks finished when it is not.
     */
    async expectHandedToBackground() {
        await expect(this.toasts.filter({ hasText: 'in the background' }).first()).toBeVisible({
            timeout: 30000
        });
        await expect(this.uploadIndicator).toBeVisible();
    }

    /** A message the author can read, whatever severity it arrived with. */
    async expectToastContaining(text: string) {
        await expect(this.toasts.filter({ hasText: text }).first()).toBeVisible({
            timeout: 60000
        });
    }

    /**
     * The outcome for a batch that has finished server-side.
     *
     * Waits on the message rather than on the listing: the run reports through a pushed signal
     * that arrives when the job resolves, and a listing that has not refreshed yet is not evidence
     * either way.
     */
    async expectOutcomeContaining(text: string) {
        await this.expectToastContaining(text);
    }

    /**
     * Whether the batch's outcome reached the author who left the portlet.
     *
     * A different surface answers this than the one inside the portlet: the toast belongs to a
     * component that is destroyed on navigation, so what survives is the durable notification, and
     * the bell is where it lands.
     */
    async expectNotificationContaining(text: string) {
        await this.page.locator('.pi-bell').first().click();

        await expect(
            this.page
                .locator('#dot-toolbar-notifications-content')
                .filter({ hasText: text })
                .first()
        ).toBeVisible({ timeout: 60000 });
    }

    /**
     * The warning that only one file will be uploaded must be gone.
     *
     * Asserted on the toast's severity rather than on its wording, so it keeps holding when the
     * copy is reworded, and fails if the old message is merely reworded rather than removed.
     */
    async expectNoSingleFileWarning() {
        await expect(this.page.locator('.p-toast-message-warn')).toHaveCount(0);
    }
}

/** A tiny in-memory PNG, so the tests carry no fixture files. */
function filePayload(name: string) {
    return { name, mimeType: 'image/png', buffer: Buffer.from([0, 0, 0, 0]) };
}
