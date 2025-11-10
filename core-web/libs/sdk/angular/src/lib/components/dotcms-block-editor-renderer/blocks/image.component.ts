import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

@Component({
    selector: 'dotcms-block-editor-renderer-image',
    template: `
        <img [alt]="attrs?.['alt']" [src]="$srcURL()" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageBlock {
    @Input() attrs!: BlockEditorNode['attrs'];

    protected readonly $srcURL = computed(() => this.attrs?.['src']);
}
