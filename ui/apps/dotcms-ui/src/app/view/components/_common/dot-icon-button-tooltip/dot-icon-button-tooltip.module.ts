import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UiDotIconButtonTooltipComponent } from './dot-icon-button-tooltip.component';
import { TooltipModule } from 'primeng/tooltip';
import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [UiDotIconButtonTooltipComponent],
    exports: [UiDotIconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule, UiDotIconButtonModule]
})
export class UiDotIconButtonTooltipModule {}
