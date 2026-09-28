import { expect, type Locator, type Page } from '@playwright/test';

/**
 * How long to wait for anything a duplicate's job has to produce.
 *
 * The same budget the upload outcomes use, for the same reason: the run is asynchronous end to end
 * (request, queued job, pushed completion), and the suite runs two workers against one instance.
 */
const OUTCOME_TIMEOUT = 60000;

/**
 * Drives folder duplication in Content Drive: the right-click item, the Action Center quick action,
 * and the places a finished duplicate has to show up.
 */
export class FolderDuplicate {
    constructor(private readonly page: Page) {}

    /** A listing row, found by its title. */
    row(title: string): Locator {
        return this.page
            .getByTestId('item-row')
            .filter({
                has: this.page.getByTestId('item-title-text').getByText(title, { exact: true })
            });
    }

    /** Duplicates one folder from its right-click menu. */
    async fromContextMenu(folderName: string) {
        await this.row(folderName).click({ button: 'right' });
        await this.page.getByRole('menuitem', { name: 'Duplicate', exact: true }).click();
    }

    /**
     * Duplicates the given folders from the Action Center.
     *
     * Duplicate has nothing to configure, so choosing it goes straight to the preview, and running
     * it from there is the whole flow.
     */
    async fromActionCenter(folderNames: string[]) {
        for (const name of folderNames) {
            await this.row(name).getByTestId('item-checkbox').click();
        }

        await this.page.getByTestId('action-center-button').click();
        await this.page.getByTestId('quick-action-DUPLICATE').click();
        await this.page.getByTestId('action-preview-execute').click();
    }

    /**
     * The duplicate is in the listing and in the sidebar tree.
     *
     * Both, because the two load separately, and a tree that has not caught up with the listing is
     * the failure only a real browser can see.
     */
    async expectDuplicateShown(duplicateName: string) {
        await expect(this.row(duplicateName)).toBeVisible({ timeout: OUTCOME_TIMEOUT });
        await expect(
            this.page
                .getByTestId('sidebar')
                .getByTestId('tree-node-label')
                .filter({ hasText: new RegExp(`^\\s*${duplicateName}\\s*$`) })
        ).toBeVisible({ timeout: OUTCOME_TIMEOUT });
    }
}
