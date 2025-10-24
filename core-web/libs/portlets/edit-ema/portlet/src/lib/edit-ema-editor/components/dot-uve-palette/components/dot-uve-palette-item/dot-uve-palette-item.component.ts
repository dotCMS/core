import {
    ChangeDetectionStrategy,
    Component,
    HostBinding,
    HostListener,
    input,
    output
} from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-uve-palette-item',
    imports: [],
    templateUrl: './dot-uve-palette-item.component.html',
    styleUrl: './dot-uve-palette-item.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[attr.data-type]': '"content-type"',
        '[attr.draggable]': 'true'
    }
})
export class DotUvePaletteItemComponent {
    $view = input<'grid' | 'list'>('grid', { alias: 'view' });
    $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });

    selectContentType = output<string>();

    readonly rightClick = output<MouseEvent>();

    @HostBinding('class.list-view')
    get isListView() {
        return this.$view() === 'list';
    }

    @HostBinding('attr.data-item')
    get dataItem() {
        const contentType = this.$contentType();
        return JSON.stringify({
            contentType: {
                variable: contentType.variable,
                name: contentType.name,
                baseType: contentType.baseType
            },
            move: false
        });
    }

    @HostListener('contextmenu', ['$event'])
    protected onRightClick(event: MouseEvent) {
        event.preventDefault();
        this.rightClick.emit(event);
    }
}
