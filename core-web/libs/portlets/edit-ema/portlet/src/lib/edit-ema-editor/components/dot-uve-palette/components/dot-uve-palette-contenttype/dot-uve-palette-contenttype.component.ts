import {
    ChangeDetectionStrategy,
    Component,
    computed,
    HostListener,
    input,
    output
} from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotPaletteViewMode } from '../../models';

@Component({
    selector: 'dot-uve-palette-contenttype',
    imports: [],
    templateUrl: './dot-uve-palette-contenttype.component.html',
    styleUrl: './dot-uve-palette-contenttype.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': 'true',
        '[attr.data-item]': '$dataItem()',
        '[class]': '$hostClass()'
    }
})
export class DotUVEPaletteContenttypeComponent {
    $view = input<DotPaletteViewMode>('grid', { alias: 'view' });
    $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });

    readonly onSelectContentType = output<string>();
    readonly contextMenu = output<MouseEvent>();

    readonly $isListView = computed(() => this.$view() === 'list');
    readonly $hostClass = computed(() => {
        const base =
            'group flex w-full items-center border border-gray-200 bg-white text-gray-900 h-auto' +
            'hover:border-[var(--color-palette-primary-500)] hover:bg-[var(--color-palette-primary-100)] hover:shadow-sm ' +
            'rounded-md';

        const grid = 'px-2 justify-center';
        const list = 'h-16 px-2 justify-between gap-2';

        return `${base} ${this.$isListView() ? list : grid}`;
    });

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
