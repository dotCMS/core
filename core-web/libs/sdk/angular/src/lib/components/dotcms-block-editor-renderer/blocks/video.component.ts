import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

@Component({
    selector: 'dotcms-block-editor-renderer-video',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <video
            [controls]="true"
            preload="metadata"
            [poster]="this.$posterURL()"
            [width]="attrs?.['width']"
            [height]="attrs?.['height']">
            <track default kind="captions" srclang="en" />
            <source [src]="this.$srcURL()" [type]="attrs?.['mimeType']" />
            Your browser does not support the
            <code>video</code>
            element.
        </video>
    `
})
export class DotVideoBlock {
    @Input() attrs!: BlockEditorNode['attrs'];

    protected readonly $srcURL = computed(() => this.attrs?.['src']);

    protected readonly $posterURL = computed(() => this.attrs?.['data']?.['thumbnail']);
}
