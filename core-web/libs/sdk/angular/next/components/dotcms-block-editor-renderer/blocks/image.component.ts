import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { ContentNode } from '@dotcms/types';

@Component({
    selector: 'dotcms-block-editor-renderer-image',
    standalone: true,
    template: `
        <img [alt]="attrs?.['alt']" [src]="$srcURL()" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageBlock {
    @Input() attrs!: ContentNode['attrs'];

    protected readonly $srcURL = computed(() => this.attrs?.['src']);
}
