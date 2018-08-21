import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconButtonTooltipComponent } from './icon-button-tooltip.component';
import { TooltipModule } from 'primeng/primeng';
import { DotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [IconButtonTooltipComponent],
    exports: [IconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule, DotIconButtonModule]
})
export class IconButtonTooltipModule {}
