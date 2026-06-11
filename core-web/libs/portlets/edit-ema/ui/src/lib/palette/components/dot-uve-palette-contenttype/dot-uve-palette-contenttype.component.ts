import { cn } from '@primeuix/utils';

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ClassNames } from 'primeng/classnames';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCMSPaletteContentType, DotPaletteViewMode } from '../../models';

@Component({
    selector: 'dot-uve-palette-contenttype',
    imports: [ClassNames, TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-palette-contenttype.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': '$draggable()',
        '[attr.data-item]': '$dataItem()',
        '[class]': '$hostClass()',
        '[class.disabled]': '$isDisabled()',
        '(click)': 'onHostClick()',
        '(contextmenu)': 'onContextMenu($event)'
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
    /**
     * Host classes built with `cn` (tailwind-merge): base + state-dependent classes are combined
     * into one string, and conflicting utilities resolve correctly — e.g. the selected
     * `bg-primary-100`/`border-primary-500` override the base `bg-white`/`border`.
     */
    readonly $hostClass = computed(() => {
        const isDisabled = this.$isDisabled();
        const selectable = this.$selectable();

        return cn(
            'group flex h-auto w-full min-w-0 items-center rounded-md border bg-white text-gray-900',
            'hover:border-primary-500 hover:bg-primary-100 hover:shadow-sm',
            // Keep the content centered, but place action icons at the sides.
            this.$isListView()
                ? 'h-16 px-2 justify-between gap-2'
                : 'px-2 py-2 justify-between gap-2',
            isDisabled && 'cursor-not-allowed opacity-60 pointer-events-none',
            selectable && !isDisabled && 'cursor-pointer',
            selectable && this.$selected()
                ? 'border-primary-500 bg-primary-100 shadow-sm'
                : 'border-gray-200'
        );
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

    protected onHostClick() {
        if (!this.$selectable() || this.$isDisabled()) {
            return;
        }
        this.onSelectContentType.emit(this.$contentType().variable);
    }

    protected onContextMenu(event: MouseEvent) {
        event.preventDefault();
        this.contextMenu.emit(event);
    }
}
