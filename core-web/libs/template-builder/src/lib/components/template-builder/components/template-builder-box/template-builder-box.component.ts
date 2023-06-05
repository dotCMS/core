import { GridItemHTMLElement } from 'gridstack';

import { NgClass, NgFor, NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotTemplateBuilderContainer, TemplateBuilderBoxSize } from '../../models/models';
import { getBoxSize } from '../../utils/gridstack-utils';

@Component({
    selector: 'dotcms-template-builder-box',
    templateUrl: './template-builder-box.component.html',
    styleUrls: ['./template-builder-box.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [NgFor, NgIf, NgClass, ButtonModule, ScrollPanelModule]
})
export class TemplateBuilderBoxComponent implements OnChanges {
    protected readonly templateBuilderSizes = TemplateBuilderBoxSize;

    @Output()
    editStyle: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    addContainer: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteContainer: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteColumn: EventEmitter<void> = new EventEmitter<void>();

    @Input() items: DotTemplateBuilderContainer[];

    @Input() size = 1;

    boxSize = TemplateBuilderBoxSize.small;

    constructor(private el: ElementRef) {}

    get nativeElement(): GridItemHTMLElement {
        return this.el.nativeElement;
    }

    ngOnChanges(): void {
        this.boxSize = getBoxSize(this.size);
    }
}
