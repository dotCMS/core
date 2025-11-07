import {
    ChangeDetectionStrategy,
    Component,
    computed,
    HostListener,
    input,
    output
} from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-uve-palette-contenttype',
    imports: [],
    templateUrl: './dot-uve-palette-contenttype.component.html',
    styleUrl: './dot-uve-palette-contenttype.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': 'true',
        '[class.list-view]': '$isListView()',
        '[attr.data-item]': '$dataItem()'
    }
})
export class DotUVEPaletteContenttypeComponent {
    $view = input<'grid' | 'list'>('grid', { alias: 'view' });
    $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });

    readonly onSelectContentType = output<string>();
    readonly contextMenu = output<MouseEvent>();

    readonly $isListView = computed(() => this.$view() === 'list');
    readonly $dataItem = computed(() => {
        const contentType = this.$contentType();

        return JSON.stringify({
            contentType: {
                variable: contentType.variable,
                name: contentType.name,
                baseType: contentType.baseType
            },
            move: false
        });
    });

    @HostListener('contextmenu', ['$event'])
    protected onContextMenu(event: MouseEvent) {
        event.preventDefault();
        this.contextMenu.emit(event);
    }
}
