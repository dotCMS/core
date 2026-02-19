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

import { DotCMSPaletteContentType } from '../../models';

@Component({
    selector: 'dot-uve-palette-contenttype',
    imports: [TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-palette-contenttype.component.html',
    styleUrl: './dot-uve-palette-contenttype.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': '$draggable()',
        '[class.list-view]': '$isListView()',
        '[class.disabled]': '$isDisabled()',
        '[attr.data-item]': '$dataItem()'
    }
})
export class DotUVEPaletteContenttypeComponent {
    $view = input<'grid' | 'list'>('grid', { alias: 'view' });
    $contentType = input.required<DotCMSPaletteContentType>({ alias: 'contentType' });

    readonly onSelectContentType = output<string>();
    readonly contextMenu = output<MouseEvent>();

    readonly $isListView = computed(() => this.$view() === 'list');
    readonly $isDisabled = computed(() => !!this.$contentType().disabled);
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
