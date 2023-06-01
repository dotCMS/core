import { NgClass, NgFor, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotTemplateBuilderContainer } from '../../models/models';

export enum TemplateBuilderBoxSize {
    large = 'large',
    medium = 'medium',
    small = 'small'
}

@Component({
    selector: 'dotcms-template-builder-box',
    templateUrl: './template-builder-box.component.html',
    styleUrls: ['./template-builder-box.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [NgFor, NgIf, NgClass, ButtonModule, ScrollPanelModule]
})
export class TemplateBuilderBoxComponent {
    @Output()
    editStyle: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    addContainer: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteContainer: EventEmitter<void> = new EventEmitter<void>();
    @Output()
    deleteColumn: EventEmitter<void> = new EventEmitter<void>();

    @Input() items: DotTemplateBuilderContainer[];

    protected readonly templateBuilderSizes = TemplateBuilderBoxSize;
    @Input() size: TemplateBuilderBoxSize = TemplateBuilderBoxSize.large;
}
