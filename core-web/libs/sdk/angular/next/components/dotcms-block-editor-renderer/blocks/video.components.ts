import { Component, Input } from '@angular/core';

import { DotCmsClient } from '@dotcms/client';
import { ContentNode } from '@dotcms/uve/internal';

@Component({
    selector: 'dotcms-block-editor-renderer-video',
    standalone: true,
    template: `
        <video
            [controls]="true"
            preload="metadata"
            [poster]="posterUrl"
            [width]="attrs?.['width']"
            [height]="attrs?.['height']">
            <track default kind="captions" srclang="en" />
            <source [src]="srcUrl" [type]="attrs?.['mimeType']" />
            Your browser does not support the
            <code>video</code>
            element.
        </video>
    `
})
export class DotCMSBlockEditorRendererVideoComponent {
    @Input() attrs!: ContentNode['attrs'];

    private client = DotCmsClient.instance;

    get srcUrl(): string {
        return this.attrs?.['data']?.['identifier']
            ? `${this.client.dotcmsUrl}${this.attrs['src']}`
            : this.attrs?.['src'];
    }

    get posterUrl(): string {
        return this.attrs?.['data']?.['thumbnail']
            ? `${this.client.dotcmsUrl}${this.attrs['data']['thumbnail']}`
            : 'poster-image.jpg';
    }
}
