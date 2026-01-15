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

import { DotCMSPaletteContentType, DotPaletteViewMode} from '../../models';

@Component({
    selector: 'dot-uve-palette-contenttype',
    imports: [NgClass, TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-palette-contenttype.component.html',
    styleUrl: './dot-uve-palette-contenttype.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': '$draggable()',
        '[attr.data-item]': '$dataItem()',
        '[class]': '$hostClass()',
        '[class.disabled]': '$isDisabled()',
    }
})
export class DotUVEPaletteContenttypeComponent {
    $view = input<DotPaletteViewMode>('grid', { alias: 'view' });
    $contentType = input.required<DotCMSPaletteContentType>({ alias: 'contentType' });
    readonly onSelectContentType = output<string>();
    readonly contextMenu = output<MouseEvent>();

    readonly $isListView = computed(() => this.$view() === 'list');
    readonly $hostClass = computed(() => {
        const isDisabled = this.$isDisabled();
        const base =
            'group flex w-full items-center border border-gray-200 bg-white text-gray-900 h-auto' +
            'hover:border-[var(--color-palette-primary-500)] hover:bg-[var(--color-palette-primary-100)] hover:shadow-sm ' +
            'rounded-md';

        // Keep the content centered, but place action icons at the sides.
        const grid = 'py-2 px-2 justify-between gap-2';
        const list = 'h-16 px-2 justify-between gap-2';

        const disabled = isDisabled ? 'cursor-not-allowed opacity-60 pointer-events-none' : '';

        return `${base} ${this.$isListView() ? list : grid} ${disabled}`;
    });

    readonly $isDisabled = computed(() => this.$contentType().disabled);
    readonly $draggable = computed(() => !this.$isDisabled());
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

    @HostListener('contextmenu', ['$event'])
    protected onContextMenu(event: MouseEvent) {
        event.preventDefault();
        this.contextMenu.emit(event);
    }
}
