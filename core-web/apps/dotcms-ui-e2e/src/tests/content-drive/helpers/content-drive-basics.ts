import { ContentDrivePage } from '@pages';
import { expect, type Locator, type Page } from '@playwright/test';

/**
 * The everyday Content Drive flows the basic-flow specs drive: selecting rows, firing a workflow
 * action through the Workflow Center, the New menu, the folder dialog and the filter chips.
 *
 * Locators only, plus the waits each step needs; what a flow should produce is asserted in the
 * specs.
 */
export class ContentDriveBasics {
    readonly drive: ContentDrivePage;
    readonly actionCenter: Locator;
    readonly folderDialogSubmit: Locator;

    constructor(private page: Page) {
        this.drive = new ContentDrivePage(page);
        this.actionCenter = page.getByTestId('action-center-dialog');
        this.folderDialogSubmit = page.getByTestId('content-drive-dialog-folder-create');
    }

    /** Ticks a row's selection checkbox. */
    async selectRow(title: string) {
        await this.drive.row(title).getByTestId('item-checkbox').click();
    }

    /**
     * Fires a workflow action on the selection through the Workflow Center, and waits for the
     * dialog to close once it ran.
     *
     * @param actionName The action's name as the scheme lists it, e.g. `Publish`.
     */
    async runWorkflowAction(actionName: string) {
        await this.drive.toolbar.getByTestId('action-center-button').click();
        await expect(this.actionCenter).toBeVisible({ timeout: 10000 });

        // Each scheme is an accordion panel, and none is open until one is picked. A click that
        // lands while the schemes are still rendering is lost, so opening is retried until the
        // action shows.
        const schemes = this.actionCenter.getByTestId('workflow-schemes');
        const action = schemes.getByText(actionName, { exact: true });
        const schemeHeader = schemes.getByRole('button', { name: /^System Workflow/ });
        await expect(async () => {
            if ((await schemeHeader.getAttribute('aria-expanded')) !== 'true') {
                await schemeHeader.click();
            }
            await expect(action).toBeVisible({ timeout: 2000 });
        }).toPass({ timeout: 15000 });
        await action.click();
        await this.actionCenter.getByTestId('action-center-continue').click();

        // Actions that take input (a comment, a destination) stop at a configuration screen first.
        const execute = this.actionCenter.getByTestId('action-preview-execute');
        const configure = this.actionCenter.getByTestId('action-configure-continue');
        await expect(execute.or(configure)).toBeVisible({ timeout: 10000 });
        if (await configure.isVisible()) {
            await configure.click();
        }

        await execute.click();
        await expect(this.actionCenter).toBeHidden({ timeout: 30000 });
    }

    /** A row's status tag, e.g. `Published`. */
    status(title: string): Locator {
        return this.drive.row(title).getByTestId('item-status');
    }

    /** Opens the New menu and picks an entry, e.g. `Folder` or `All Content Types`. */
    async chooseNew(entry: string) {
        await this.drive.toolbar.getByTestId('add-new-button').click();
        await this.page.getByRole('menuitem', { name: entry, exact: true }).click();
    }

    /** In the content type picker the New menu opens, picks a type and confirms. */
    async pickContentType(name: string) {
        await this.page.getByTestId('palette-search-input').fill(name);
        await this.page.getByTestId('content-type-name').filter({ hasText: name }).first().click();
        await this.page.getByTestId('content-type-selector-create').click();
    }

    /** The folder dialog's URL name field. */
    get folderNameField(): Locator {
        return this.page.getByLabel('Name (URL)');
    }

    /** Opens a row's right-click menu and picks an entry. */
    async rightClickAction(title: string, entry: string) {
        await this.drive.row(title).getByTestId('item-title-text').click({ button: 'right' });
        await this.page.getByRole('menuitem', { name: entry, exact: true }).click();
    }

    /** Opens a filter chip and ticks one option in its panel. */
    async chooseStatus(label: string) {
        await this.page.getByTestId('status-filter-chip').click();
        const panel = this.page.getByTestId('status-filter-panel');
        await panel.getByText(label, { exact: true }).click();
        await this.page.keyboard.press('Escape');
    }

    /** Opens the Content Types chip, searches for a type and ticks it. */
    async chooseContentType(name: string) {
        await this.page.getByTestId('content-type-filter-chip').click();
        const panel = this.page.getByTestId('content-type-filter-panel');
        await panel.getByTestId('content-type-search').fill(name);
        await panel.getByTestId('content-type-item').filter({ hasText: name }).first().click();
        await this.page.keyboard.press('Escape');
    }
}
