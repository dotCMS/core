import { NgFor, NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { GRID_STACK_MARGIN_HORIZONTAL, GRID_STACK_UNIT } from '../../utils/gridstack-options';

@Component({
    selector: 'dotcms-template-builder-background-columns',
    templateUrl: './template-builder-background-columns.component.html',
    styleUrls: ['./template-builder-background-columns.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [NgFor, NgStyle]
})
export class TemplateBuilderBackgroundColumnsComponent {
    readonly columnList = [].constructor(12);
    readonly gridStackGap = `${GRID_STACK_MARGIN_HORIZONTAL * 2}${GRID_STACK_UNIT}`;
}
