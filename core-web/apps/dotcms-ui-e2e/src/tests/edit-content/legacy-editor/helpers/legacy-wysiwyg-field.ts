import { expect, type Frame, type FrameLocator, type Locator } from '@playwright/test';

/**
 * Locator wrapper for a WYSIWYG field on the legacy (Dojo/JSP) editor.
 *
 * The legacy editor runs TinyMCE 4 inside the edit_contentlet iframe: the toolbar and its windows
 * are regular DOM in that frame, while the document being edited is a nested iframe named
 * `<variable>_ifr`. Anything asserting on inserted content goes through {@link body}.
 */
export class LegacyWysiwygField {
    readonly body: FrameLocator;
    readonly insertLinkButton: Locator;

    constructor(
        private frame: Frame,
        readonly fieldVariable: string
    ) {
        this.body = frame.frameLocator(`#${fieldVariable}_ifr`);
        // TinyMCE 4 toolbar buttons are `div[role=button]` elements named by their tooltip.
        this.insertLinkButton = frame.getByRole('button', { name: 'Insert/edit link' }).first();
    }

    /** Waits for TinyMCE to finish booting; the toolbar is the tightest signal available. */
    async expectReady(): Promise<void> {
        await expect(this.insertLinkButton).toBeVisible({ timeout: 30000 });
    }

    /** Selects a whole paragraph by its text, the way an author does before clicking Link. */
    async selectParagraph(text: string): Promise<void> {
        await this.body.getByText(text, { exact: true }).click({ clickCount: 3 });
    }

    /** Opens the "Insert link" window from the toolbar. */
    async openLinkDialog(): Promise<LinkDialog> {
        await this.insertLinkButton.click();

        const root = this.frame.locator('.mce-window', { hasText: 'Insert link' });
        await expect(root).toBeVisible();

        return new LinkDialog(this.frame, root);
    }

    /** The links in the document being edited. */
    get links(): Locator {
        return this.body.getByRole('link');
    }
}

/** TinyMCE 4's "Insert link" window. */
export class LinkDialog {
    readonly urlInput: Locator;
    readonly browseButton: Locator;
    readonly okButton: Locator;

    constructor(
        private frame: Frame,
        readonly root: Locator
    ) {
        // The Url field is a TinyMCE "filepicker": a text box plus a browse control whose element id
        // ends in `-open` (that suffix is what the combobox's click handler keys on). The Url box is
        // the window's first text box and has autofocus.
        this.urlInput = root.locator('input.mce-textbox').first();
        this.browseButton = root.locator('[id$="-open"]').first();
        this.okButton = root.getByRole('button', { name: 'Ok' }).first();
    }

    /** Clicks browse, which the editor routes to the Dojo file browser wired as `cmsFileBrowserFile`. */
    async browse(): Promise<FileBrowserDialog> {
        await this.browseButton.click();

        const root = this.frame.getByRole('dialog', { name: 'Select a file' });
        await expect(root).toBeVisible({ timeout: 15000 });

        return new FileBrowserDialog(root);
    }
}

/** The Dojo `dotcms.dijit.FileBrowserDialog` the Link window opens for the Url field. */
export class FileBrowserDialog {
    constructor(readonly root: Locator) {}

    /** Clicks a site in the folder tree; the file list reloads with that site's root content. */
    async selectSite(hostname: string): Promise<void> {
        await this.root.getByText(hostname, { exact: true }).click();
    }

    /**
     * Picks a file row. Rows are rendered with `id="file-<identifier>"` and then suffixed with their
     * index, so the identifier prefix is the stable handle. Selecting a file closes the dialog.
     */
    async pick(identifier: string): Promise<void> {
        const row = this.root.locator(`[id^="file-${identifier}"]`);
        await expect(row).toBeVisible({ timeout: 15000 });
        await row.locator('.selectableFile').click();
        await expect(this.root).toBeHidden({ timeout: 15000 });
    }
}
