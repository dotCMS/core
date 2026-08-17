import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

@Component({
    selector: 'dotcms-block-editor-renderer-image',
    imports: [NgTemplateOutlet],
    template: `
        <figure [style]="$wrapperStyle()">
            @if ($href(); as href) {
                <!-- target/rel are attribute bindings so a null value omits the
                     attribute instead of rendering the string "null". -->
                <a [href]="href" [attr.target]="$target()" [attr.rel]="$rel()">
                    <ng-container [ngTemplateOutlet]="image" />
                </a>
            } @else {
                <ng-container [ngTemplateOutlet]="image" />
            }
        </figure>

        <ng-template #image>
            <img [alt]="attrs()?.['alt']" [src]="$srcURL()" />
        </ng-template>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageBlock {
    readonly attrs = input<BlockEditorNode['attrs']>();

    protected readonly $srcURL = computed(() => this.attrs()?.['src']);

    /**
     * Link assigned to the image in the Block Editor, stored as `href` on the
     * `dotImage` node. `null` when never set, `''` after the editor unsets it —
     * both mean "no link", so the image renders without an anchor.
     */
    protected readonly $href = computed(() => this.attrs()?.['href'] || null);

    protected readonly $target = computed(() => this.attrs()?.['target'] || null);

    /** Guards against reverse tabnabbing when the link opens in a new tab. */
    protected readonly $rel = computed(() =>
        this.$target() === '_blank' ? 'noopener noreferrer' : null
    );

    protected readonly $wrapperStyle = computed(() => {
        const textWrap = this.attrs()?.['textWrap'];
        const textAlign = this.attrs()?.['textAlign'];

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
