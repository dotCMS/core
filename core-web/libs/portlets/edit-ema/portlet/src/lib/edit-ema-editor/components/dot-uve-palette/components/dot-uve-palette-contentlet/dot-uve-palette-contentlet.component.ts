import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    HostBinding,
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
        '[attr.draggable]': 'true'
    }
})
export class DotUvePaletteContentletComponent {
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    @HostBinding('attr.data-item')
    get dataItem() {
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
    }
}
