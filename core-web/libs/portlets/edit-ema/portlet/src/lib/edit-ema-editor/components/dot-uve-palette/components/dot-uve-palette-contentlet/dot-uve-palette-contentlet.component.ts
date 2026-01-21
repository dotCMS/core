import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    input
} from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-uve-palette-contentlet',
    imports: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './dot-uve-palette-contentlet.component.html',
    styleUrl: './dot-uve-palette-contentlet.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"contentlet"',
        '[attr.draggable]': 'true',
        '[attr.data-item]': '$dataItem()'
    }
})
export class DotUvePaletteContentletComponent {
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    readonly $dataItem = computed(() => {
        const contentlet = this.$contentlet();

        return JSON.stringify({
            contentlet: {
                identifier: contentlet.identifier,
                contentType: contentlet.contentType,
                baseType: contentlet.baseType,
                inode: contentlet.inode,
                title: contentlet.title
            },
            move: false
        });
    });
}
