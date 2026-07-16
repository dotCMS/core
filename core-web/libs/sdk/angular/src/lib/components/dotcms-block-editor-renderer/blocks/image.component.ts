import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

@Component({
    selector: 'dotcms-block-editor-renderer-image',
    imports: [],
    template: `
        <figure [style]="$wrapperStyle()">
            <img [alt]="attrs?.['alt']" [src]="$srcURL()" />
        </figure>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageBlock {
    @Input() attrs!: BlockEditorNode['attrs'];

    protected readonly $srcURL = computed(() => this.attrs?.['src']);

    protected readonly $wrapperStyle = computed(() => {
        const textWrap = this.attrs?.['textWrap'];
        const textAlign = this.attrs?.['textAlign'];

        if (textWrap === 'left') {
            return { float: 'left', width: '50%', margin: '0 1rem 1rem 0' };
        } else if (textWrap === 'right') {
            return { float: 'right', width: '50%', margin: '0 0 1rem 1rem' };
        } else if (textAlign) {
            return { 'text-align': textAlign };
        }

        return {};
    });
}
