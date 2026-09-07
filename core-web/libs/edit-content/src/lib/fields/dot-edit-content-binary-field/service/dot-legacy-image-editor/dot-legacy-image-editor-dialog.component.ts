import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { SafeUrlPipe } from '@dotcms/ui';

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

type LegacyImageEditorDialogConfig = { data: DotLegacyImageEditorDialogData };

/**
 * Renders the legacy Dojo image editor inside a PrimeNG dialog iframe.
 *
 * Builds a URL to `image-editor-standalone.jsp` with the inode, temp file, and field
 * variable required by the legacy editor; marked trusted via SafeUrlPipe
 * (`bypassSecurityTrustResourceUrl`) so Angular renders it as the iframe src.
 */
@Component({
    selector: 'dot-legacy-image-editor-dialog',
    template: `
        <iframe
            [src]="$iframeUrl() | safeUrl"
            class="block size-full border-0"
            data-testid="legacy-image-editor-iframe"
            frameborder="0"
            title="Image editor"></iframe>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block h-full w-full' },
    imports: [SafeUrlPipe]
})
export class DotLegacyImageEditorDialogComponent {
    readonly #dialogData: DotLegacyImageEditorDialogData = (
        inject(DynamicDialogConfig) as LegacyImageEditorDialogConfig
    ).data;

    /**
     * Raw iframe URL for the standalone legacy image editor JSP; marked trusted in the
     * template via SafeUrlPipe (`bypassSecurityTrustResourceUrl`).
     */
    readonly $iframeUrl = computed(() => {
        const { inode, tempId, variable } = this.#dialogData;
        const params = new URLSearchParams();

        if (inode) {
            params.set('inode', inode);
        }

        if (tempId) {
            params.set('tempId', tempId);
        }

        params.set('fieldName', variable);
        params.set('variable', variable);

        return `${IMAGE_EDITOR_STANDALONE_JSP}?${params.toString()}`;
    });
}
