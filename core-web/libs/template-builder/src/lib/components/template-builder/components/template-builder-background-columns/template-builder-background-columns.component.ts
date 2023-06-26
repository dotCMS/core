import { NgClass, NgFor, NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, Input } from '@angular/core';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';

import { GRID_STACK_MARGIN_HORIZONTAL, GRID_STACK_UNIT } from '../../utils/gridstack-options';

@Component({
    selector: 'dotcms-template-builder-background-columns',
    templateUrl: './template-builder-background-columns.component.html',
    styleUrls: ['./template-builder-background-columns.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [NgFor, NgStyle, NgClass]
})
export class TemplateBuilderBackgroundColumnsComponent {
    @Input() show = true;

    readonly columnList = [].constructor(12);
    readonly gridStackGap = `${GRID_STACK_MARGIN_HORIZONTAL * 2}${GRID_STACK_UNIT}`;

    @HostBinding('style')
    hostStyle: SafeStyle;

    constructor(private sanitizer: DomSanitizer) {
        this.hostStyle = this.sanitizer.bypassSecurityTrustStyle(`gap: ${this.gridStackGap}`);
    }
}
