import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';
import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipComponent } from './dot-icon-button-tooltip.component';

@NgModule({
    declarations: [UiDotIconButtonTooltipComponent],
    exports: [UiDotIconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule, UiDotIconButtonModule]
})
export class UiDotIconButtonTooltipModule {}
