import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { contentletToThumbnailModel, DotContentThumbnailComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-palette-contentlet',
    imports: [TooltipModule, DotContentThumbnailComponent],
    templateUrl: './dot-uve-palette-contentlet.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"contentlet"',
        '[attr.draggable]': 'true',
        '[attr.data-item]': '$dataItem()',
        '[class]': '$hostClass'
    }
})
export class DotUvePaletteContentletComponent {
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    readonly $thumbnail = computed(() => contentletToThumbnailModel(this.$contentlet()));

    readonly $isIconThumbnail = computed(() => this.$thumbnail().type === 'icon');

    readonly $hostClass =
        'group flex w-full items-center overflow-hidden cursor-grab active:cursor-grabbing ' +
        'h-16 rounded-md border border-gray-200 bg-white text-gray-900 ' +
        'hover:border-primary-500 hover:bg-primary-100 hover:shadow-sm';

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
