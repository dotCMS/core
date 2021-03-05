import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIconButtonTooltipComponent } from './dot-icon-button-tooltip.component';
import { TooltipModule } from 'primeng/tooltip';
import { DotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [DotIconButtonTooltipComponent],
    exports: [DotIconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule, DotIconButtonModule]
})
export class DotIconButtonTooltipModule {}
