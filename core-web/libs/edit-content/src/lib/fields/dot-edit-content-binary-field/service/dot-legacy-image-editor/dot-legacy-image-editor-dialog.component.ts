import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

export interface DotLegacyImageEditorDialogData {
    inode?: string;
    tempId?: string;
    variable: string;
}

const IMAGE_EDITOR_STANDALONE_JSP = '/html/js/dotcms/dijit/image/image-editor-standalone.jsp';

@Component({
    selector: 'dot-legacy-image-editor-dialog',
    template: `
        <iframe
            [src]="$iframeSrc()"
            class="legacy-image-editor__iframe"
            data-testid="legacy-image-editor-iframe"
            frameborder="0"
            title="Image editor"></iframe>
    `,
    styles: [
        `
            :host {
                display: block;
                height: 100%;
                width: 100%;
            }

            .legacy-image-editor__iframe {
                border: none;
                display: block;
                height: 100%;
                width: 100%;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLegacyImageEditorDialogComponent {
    readonly #dialogConfig = inject(DynamicDialogConfig<DotLegacyImageEditorDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

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
