import { NgClass, NgFor, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainers } from '../../models/models';

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
    @Input() items: DotContainers;

    protected readonly templateBuilderSizes = TemplateBuilderBoxSize;
    @Input() size: TemplateBuilderBoxSize = TemplateBuilderBoxSize.large;
}
