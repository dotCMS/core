import { GridItemHTMLElement } from 'gridstack';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dotcms-template-builder-row',
    standalone: true,
    imports: [ButtonModule],
    templateUrl: './template-builder-row.component.html',
    styleUrls: ['./template-builder-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderRowComponent {
    @Output()
    editStyleClasses: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteRow: EventEmitter<void> = new EventEmitter<void>();

    constructor(private el: ElementRef) {}

    get nativeElement(): GridItemHTMLElement {
        return this.el.nativeElement;
    }
}
