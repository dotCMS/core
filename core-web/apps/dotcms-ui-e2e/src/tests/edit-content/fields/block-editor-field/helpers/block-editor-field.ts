import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Locator wrapper for the Block Editor (Story Block) field — `dot-edit-content-block-editor`
 * rendering `dot-block-editor` from `libs/new-block-editor`.
 *
 * Scopes everything to `data-testid="field-{variable}"`. The editing surface is the toolbar's
 * sibling, exposed as `role="textbox"` with `aria-multiline`, so it is reachable by role rather than
 * by the ProseMirror class.
 */
export class BlockEditorField {
    readonly root: Locator;
    readonly editor: Locator;
    readonly content: Locator;
    readonly insertImageButton: Locator;
    readonly insertVideoButton: Locator;
    readonly insertAudioButton: Locator;

    constructor(
        private page: Page,
        readonly fieldVariable = 'blockEditorField'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
        this.editor = this.root.locator('dot-block-editor');
        this.content = this.root.getByRole('textbox');
        // `exact` matters: "Edit image properties" and "Insert asset by URL" also live in this
        // toolbar, and a substring match would make these ambiguous the moment a label changes.
        this.insertImageButton = this.root.getByRole('button', {
            name: 'Insert image',
            exact: true
        });
        this.insertVideoButton = this.root.getByRole('button', {
            name: 'Insert video',
            exact: true
        });
        this.insertAudioButton = this.root.getByRole('button', {
            name: 'Insert audio',
            exact: true
        });
    }

    /**
     * Waits for the NEW block editor to be the one on screen.
     *
     * `FEATURE_FLAG_NEW_BLOCK_EDITOR` resolves to `true` when unset, so this is the default — but if
     * an environment has it explicitly `false` the legacy `dot-old-block-editor` renders instead and
     * every locator here silently finds nothing. Failing on this element gives that a name.
     */
    async expectVisible(): Promise<void> {
        await expect(this.editor).toBeVisible({ timeout: 20000 });
        await expect(this.content).toBeVisible({ timeout: 15000 });
    }

    /**
     * Opens the shared AssetPicker from a toolbar button and waits for its first result page.
     *
     * Two requests happen between the click and a usable dialog: the picker needs the current site
     * before it can be configured, and only then does it search. Waiting on the search covers both —
     * it cannot fire until the dialog has mounted.
     */
    async openAssetPicker(button: Locator): Promise<void> {
        const searchResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/drive/search') && response.status() === 200,
            { timeout: 30000 }
        );

        await button.click();
        await searchResponse;
    }

    async openImagePicker(): Promise<void> {
        await this.openAssetPicker(this.insertImageButton);
    }

    /** The images embedded in the document. */
    get images(): Locator {
        return this.content.getByRole('img');
    }

    /**
     * Asserts an image node was inserted for exactly this asset.
     *
     * Keys on the inode rather than the file name: `insertDotImageFromContentlet` builds the src as
     * `/dA/{inode}`, so this is what proves the picked row is the one that landed in the document
     * and not some other asset with a similar title.
     */
    async expectImageInserted(inode: string, title: string): Promise<void> {
        const image = this.images.first();

        await expect(image).toBeVisible({ timeout: 15000 });
        await expect(image).toHaveAttribute('src', new RegExp(inode));
        await expect(image).toHaveAttribute('alt', title);
    }

    async expectNoImages(): Promise<void> {
        await expect(this.images).toHaveCount(0);
    }
}
