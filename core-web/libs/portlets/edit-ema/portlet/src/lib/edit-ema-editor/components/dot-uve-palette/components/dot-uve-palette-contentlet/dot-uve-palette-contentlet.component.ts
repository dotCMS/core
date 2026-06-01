import { NgClass } from '@angular/common';
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
    imports: [NgClass],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './dot-uve-palette-contentlet.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"contentlet"',
        '[attr.draggable]': 'true',
        '[attr.data-item]': '$dataItem()',
        '[attr.title]': '$contentlet().title',
        '[class]': '$hostClass'
    }
})
export class DotUvePaletteContentletComponent {
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    readonly $isIconThumbnail = computed(() => {
        const c = this.$contentlet();
        const mime = c?.mimeType ?? '';
        const renderImage =
            !!c?.['hasTitleImage'] ||
            mime === 'application/pdf' ||
            !!c?.['image'] ||
            mime.includes('video');

        return !renderImage;
    });

    readonly $hostClass =
        'group flex w-full items-center overflow-hidden cursor-grab active:cursor-grabbing ' +
        'min-h-16 rounded-md border border-gray-200 bg-white text-gray-900 ' +
        'hover:border-[var(--color-palette-primary-500)] hover:bg-[var(--color-palette-primary-100)] hover:shadow-sm';

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
