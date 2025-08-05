import { ChangeDetectionStrategy, Component, HostBinding, inject } from '@angular/core';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';

import { GRID_STACK_MARGIN_HORIZONTAL, GRID_STACK_UNIT } from '../../utils/gridstack-options';

@Component({
    selector: 'dotcms-template-builder-background-columns',
    templateUrl: './template-builder-background-columns.component.html',
    styleUrls: ['./template-builder-background-columns.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: []
})
export class TemplateBuilderBackgroundColumnsComponent {
    private sanitizer = inject(DomSanitizer);

    readonly columnList = [].constructor(12);
    readonly gridStackGap = `${GRID_STACK_MARGIN_HORIZONTAL * 2}${GRID_STACK_UNIT}`;

    @HostBinding('style')
    hostStyle: SafeStyle;

    constructor() {
        this.hostStyle = this.sanitizer.bypassSecurityTrustStyle(`gap: ${this.gridStackGap}`);
    }
}
