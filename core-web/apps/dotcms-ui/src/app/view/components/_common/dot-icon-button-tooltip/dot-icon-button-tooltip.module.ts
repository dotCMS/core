import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonTooltipComponent } from './dot-icon-button-tooltip.component';

/**
 * @deprecated
 */
@NgModule({
    declarations: [UiDotIconButtonTooltipComponent],
    exports: [UiDotIconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule]
})
export class UiDotIconButtonTooltipModule {}
