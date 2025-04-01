import { Component, Input } from '@angular/core';

import { DotCmsClient } from '@dotcms/client';
import { ContentNode } from '@dotcms/uve/internal';

@Component({
    selector: 'dotcms-block-editor-renderer-image',
    standalone: true,
    template: `
        <img [alt]="attrs?.['alt']" [src]="srcUrl" />
    `
})
export class DotCMSBlockEditorRendererImageComponent {
    @Input() attrs!: ContentNode['attrs'];

    private client = DotCmsClient.instance;

    get srcUrl(): string {
        return this.attrs?.['data']?.['identifier']
            ? `${this.client.dotcmsUrl}${this.attrs['src']}`
            : this.attrs?.['src'];
    }
}
