import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconButtonTooltipComponent } from './icon-button-tooltip.component';
import { TooltipModule } from 'primeng/primeng';

@NgModule({
    declarations: [IconButtonTooltipComponent],
    exports: [IconButtonTooltipComponent],
    imports: [CommonModule, TooltipModule],
})
export class IconButtonTooltipModule {}