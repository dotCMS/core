import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    HostListener,
    input,
    output
} from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCMSPaletteContentType, DotPaletteViewMode } from '../../models';

@Component({
    selector: 'dot-uve-palette-contenttype',
    imports: [NgClass, TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-palette-contenttype.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': '$draggable()',
        '[attr.data-item]': '$dataItem()',
        '[class]': '$hostClass()',
        '[class.disabled]': '$isDisabled()'
    }
})
export class DotUVEPaletteContenttypeComponent {
    $view = input<DotPaletteViewMode>('grid', { alias: 'view' });
    $contentType = input.required<DotCMSPaletteContentType>({ alias: 'contentType' });
    /**
     * Selection mode: the whole card is clickable to pick a content type, the drag
     * handle and chevron are hidden, and the card is not draggable. Used outside UVE
     * (e.g. the Content Drive "New" dialog). Defaults to false to keep UVE behavior.
     */
    $selectable = input<boolean>(false, { alias: 'selectable' });
    /** Highlights the card when it is the current selection (selection mode). */
    $selected = input<boolean>(false, { alias: 'selected' });
    readonly onSelectContentType = output<string>();
    readonly contextMenu = output<MouseEvent>();

    readonly $isListView = computed(() => this.$view() === 'list');
    readonly $hostClass = computed(() => {
        const isDisabled = this.$isDisabled();
        const selectable = this.$selectable();
        const base =
            'group flex w-full min-w-0 items-center border bg-white text-gray-900 h-auto' +
            'hover:border-primary-500 hover:bg-primary-100 hover:shadow-sm ' +
            'rounded-md';

        // Keep the content centered, but place action icons at the sides.
        const grid = 'py-2 px-2 justify-between gap-2';
        const list = 'h-16 px-2 justify-between gap-2';

        const disabled = isDisabled ? 'cursor-not-allowed opacity-60 pointer-events-none' : '';
        const selectableClass = selectable && !isDisabled ? 'cursor-pointer' : '';
        const selectedClass =
            selectable && this.$selected()
                ? 'border-primary-500 bg-primary-100 shadow-sm'
                : 'border-gray-200';

        return `${base} ${this.$isListView() ? list : grid} ${disabled} ${selectableClass} ${selectedClass}`;
    });

    readonly $isDisabled = computed(() => this.$contentType().disabled);
    readonly $draggable = computed(() => !this.$isDisabled() && !this.$selectable());
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

    protected onChevronClick(contentType: DotCMSPaletteContentType) {
        if (contentType.disabled) {
            return;
        }
        this.onSelectContentType.emit(contentType.variable);
    }

    @HostListener('click')
    protected onHostClick() {
        if (!this.$selectable() || this.$isDisabled()) {
            return;
        }
        this.onSelectContentType.emit(this.$contentType().variable);
    }

    @HostListener('contextmenu', ['$event'])
    protected onContextMenu(event: MouseEvent) {
        event.preventDefault();
        this.contextMenu.emit(event);
    }
}
