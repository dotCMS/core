import { expect, type FrameLocator, type Locator, type Page } from '@playwright/test';

/**
 * Locator wrapper for the WYSIWYG field (`dot-edit-content-wysiwyg-field`) on its default TinyMCE
 * editor.
 *
 * TinyMCE splits itself across the iframe boundary: the toolbar is regular DOM inside the field, the
 * document being edited is a separate iframe. Anything asserting on inserted content has to go
 * through {@link body}.
 */
export class WysiwygField {
    readonly root: Locator;
    readonly editorSelector: Locator;
    readonly toolbar: Locator;
    readonly insertImageButton: Locator;

    constructor(
        private page: Page,
        readonly fieldVariable = 'wysiwygField'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
        this.editorSelector = this.root.getByTestId('editor-selector');
        this.toolbar = this.root.getByRole('toolbar');
        // The button is icon-only; its accessible name comes from the `tooltip` the plugin registers
        // (TinyMCE's silver theme renders `tooltip` as both `title` and `aria-label`).
        this.insertImageButton = this.root.getByRole('button', {
            name: 'Insert Image',
            exact: true
        });
    }

    /** The document being edited, which TinyMCE keeps in its own iframe. */
    get body(): FrameLocator {
        return this.root.frameLocator('iframe');
    }

    /**
     * Waits for TinyMCE to finish booting.
     *
     * The script is loaded on demand, so the field renders its wrapper well before the toolbar
     * exists. Waiting on the button this suite clicks is the tightest signal available.
     */
    async expectVisible(): Promise<void> {
        await expect(this.insertImageButton).toBeVisible({ timeout: 30000 });
    }

    /**
     * Opens the shared AssetPicker from the toolbar and waits for its first result page.
     *
     * Two requests happen between the click and a usable dialog: the picker needs the current site
     * before it can be configured, and only then does it search. Waiting on the search covers both —
     * it cannot fire until the dialog has mounted.
     */
    async openImagePicker(): Promise<void> {
        const searchResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/drive/search') && response.status() === 200,
            { timeout: 30000 }
        );

        await this.insertImageButton.click();
        await searchResponse;
    }

    /** The images embedded in the document. */
    get images(): Locator {
        return this.body.getByRole('img');
    }

    /**
     * Asserts an `<img>` was inserted for exactly this asset.
     *
     * Keys on `data-identifier` rather than the src: the src is built from a configurable pattern
     * (`WYSIWYG_IMAGE_URL_PATTERN`), so an environment that customises it would break a src
     * assertion while the behaviour under test is still correct. The data attributes are written
     * unconditionally by `formatDotImageNode`.
     */
    async expectImageInserted(identifier: string, inode: string, title: string): Promise<void> {
        const image = this.images.first();

        await expect(image).toBeVisible({ timeout: 15000 });
        await expect(image).toHaveAttribute('data-identifier', identifier);
        await expect(image).toHaveAttribute('data-inode', inode);
        await expect(image).toHaveAttribute('alt', title);
    }

    async expectNoImages(): Promise<void> {
        await expect(this.images).toHaveCount(0);
    }

    /** Whether the toolbar button carries an accessible name — it is icon-only. */
    async expectInsertImageButtonLabelled(): Promise<void> {
        await expect(this.insertImageButton).toHaveAttribute('aria-label', 'Insert Image');
    }
}
