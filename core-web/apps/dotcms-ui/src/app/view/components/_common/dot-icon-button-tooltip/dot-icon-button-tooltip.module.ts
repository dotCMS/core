import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonTooltipComponent } from './dot-icon-button-tooltip.component';

import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

/**
 * @deprecated
 */
@NgModule({
    declarations: [UiDotIconButtonTooltipComponent],
    exports: [UiDotIconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule, UiDotIconButtonModule]
})
export class UiDotIconButtonTooltipModule {}
