import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

/**
 * Data passed to {@link DotLegacyImageEditorDialogComponent} when opening the legacy image editor.
 */
export interface DotLegacyImageEditorDialogData {
    /** Content inode when editing a published asset. */
    inode?: string;
    /** Temporary file id when editing an unsaved upload. */
    tempId?: string;
    /** Binary field variable name used for editor event routing. */
    variable: string;
}

const IMAGE_EDITOR_STANDALONE_JSP = '/html/js/dotcms/dijit/image/image-editor-standalone.jsp';

/**
 * Renders the legacy Dojo image editor inside a PrimeNG dialog iframe.
 *
 * Builds a sanitized URL to `image-editor-standalone.jsp` with the inode, temp file,
 * and field variable required by the legacy editor.
 */
@Component({
    selector: 'dot-legacy-image-editor-dialog',
    template: `
        <iframe
            [src]="$iframeSrc()"
            class="block size-full border-0"
            data-testid="legacy-image-editor-iframe"
            frameborder="0"
            title="Image editor"></iframe>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block h-full w-full' }
})
export class DotLegacyImageEditorDialogComponent {
    readonly #dialogConfig = inject(DynamicDialogConfig<DotLegacyImageEditorDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

    /**
     * Sanitized iframe URL for the standalone legacy image editor JSP.
     */
    readonly $iframeSrc = computed(() => {
        const { inode, tempId, variable } = this.#dialogConfig.data;
        const params = new URLSearchParams();

        if (inode) {
            params.set('inode', inode);
        }

        if (tempId) {
            params.set('tempId', tempId);
        }

        params.set('fieldName', variable);
        params.set('variable', variable);

        const url = `${IMAGE_EDITOR_STANDALONE_JSP}?${params.toString()}`;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });
}
