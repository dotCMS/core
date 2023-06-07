import { NgFor, NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding } from '@angular/core';

import { GRID_STACK_MARGIN_HORIZONTAL, GRID_STACK_UNIT } from '../../utils/gridstack-options';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';

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

    @HostBinding('style')
    hostStyle: SafeStyle;

    constructor(private sanitizer: DomSanitizer) {
        this.hostStyle = sanitizer.bypassSecurityTrustStyle(`gap: ${this.gridStackGap}`);
    }
}
