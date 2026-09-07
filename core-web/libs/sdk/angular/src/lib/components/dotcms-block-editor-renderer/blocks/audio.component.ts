import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

/**
 * Renders a dotCMS audio block as a native `<audio>` element. Mirrors {@link DotVideoBlock}
 * but without poster/width/height, which are not meaningful for audio.
 */
@Component({
    selector: 'dotcms-block-editor-renderer-audio',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <audio [controls]="true" preload="metadata">
            <source [src]="attrs?.['src']" [type]="attrs?.['mimeType']" />
            Your browser does not support the
            <code>audio</code>
            element.
        </audio>
    `
})
export class DotAudioBlock {
    @Input() attrs!: BlockEditorNode['attrs'];
}
