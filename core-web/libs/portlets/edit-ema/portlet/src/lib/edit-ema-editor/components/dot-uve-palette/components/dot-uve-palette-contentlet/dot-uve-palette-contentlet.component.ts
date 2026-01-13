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
        '[attr.data-item]': '$dataItem()',
        '[class]': '$hostClass()'
    }
})
export class DotUvePaletteContentletComponent {
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    readonly $hostClass = computed(() => {
        return (
            'group flex w-full items-center justify-between gap-2 overflow-hidden ' +
            'h-16 px-2 rounded-md border border-gray-200 bg-white text-gray-900 ' +
            'hover:border-[var(--color-palette-primary-500)] hover:bg-[var(--color-palette-primary-100)] hover:shadow-sm'
        );
    });

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
