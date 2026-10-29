import { iFramesLocators } from '@locators/globalLocators';
import { NewEditContentFormPage } from '@pages';
import { expect, type FrameLocator, type Locator, type Page } from '@playwright/test';

/**
 * The side panel every screen opens content in since #38303, in either editor.
 *
 * Each open panel is a PrimeNG drawer (`.p-drawer`, appended to the body) whose root carries
 * `data-testid="side-panel"`, so the test id and the drawer are the same element. Panels stack: a
 * panel opened from inside another one is added after it, so the last is the front one, the one
 * Escape and the header X close.
 */
export class SidePanel {
    /** Every open panel, back to front. */
    readonly all: Locator;

    constructor(private page: Page) {
        this.all = page.getByTestId('side-panel');
    }

    /** The front panel: the last one opened. */
    get front(): Locator {
        return this.all.last();
    }

    /** The front panel's heading: the content's title, or its type's name on a create. */
    get title(): Locator {
        return this.front.getByTestId('side-panel-title');
    }

    /** The front panel's header X. */
    get closeButton(): Locator {
        return this.front.getByTestId('side-panel-close');
    }

    /**
     * The legacy editor (edit_contentlet JSP) in the front panel. Dojo markup, so CSS selectors
     * are expected inside it.
     */
    get legacyEditor(): FrameLocator {
        return this.front.frameLocator(iFramesLocators.legacy_editor_iframe);
    }

    async expectOpen(): Promise<void> {
        await expect(this.front).toBeVisible({ timeout: 20000 });
    }

    /**
     * Whether the front panel is the topmost thing at its own centre, so nothing else (a dialog
     * opened before it, say) covers it. Asks the browser which element is hit there, rather than
     * comparing z-indexes, which live on different elements for different overlays.
     */
    async isOnTop(): Promise<boolean> {
        return this.front.evaluate((panel) => {
            const { left, top, width, height } = panel.getBoundingClientRect();
            const hit = document.elementFromPoint(left + width / 2, top + height / 2);

            return !!hit && panel.contains(hit);
        });
    }

    /** Asserts how many panels are open, stacked. */
    async expectCount(count: number): Promise<void> {
        await expect(this.all).toHaveCount(count, { timeout: 15000 });
    }

    async expectClosed(): Promise<void> {
        await this.expectCount(0);
    }

    /** Closes the front panel with its header X. */
    async close(): Promise<void> {
        await this.closeButton.click();
    }

    /**
     * The new editor's form in the front panel. Scoped to it because a panel stacked over another
     * has a twin of every field and workflow button.
     */
    get form(): NewEditContentFormPage {
        return new NewEditContentFormPage(this.page, this.front);
    }

    /**
     * Replaces the legacy editor's text field with `value` and fires its "Save" workflow action.
     *
     * The action links are `<a onclick>` without an `href`, so they have no link role; they are
     * matched by their class and exact label. The legacy editor saves through DWR and reports back
     * on a timer, so callers assert on what the save changes rather than on a request.
     *
     * @param fieldVariable The text field's variable, which is its input's id.
     * @param value The new value.
     */
    async saveLegacyTextField(fieldVariable: string, value: string): Promise<void> {
        const field = this.legacyEditor.locator(`#${fieldVariable}`);
        await expect(field).toBeVisible({ timeout: 20000 });
        await expect(field).toBeEditable();
        await field.fill(value);

        await this.legacyEditor.locator('a.schemeActionsDiv', { hasText: /^\s*Save\s*$/ }).click();
    }
}
