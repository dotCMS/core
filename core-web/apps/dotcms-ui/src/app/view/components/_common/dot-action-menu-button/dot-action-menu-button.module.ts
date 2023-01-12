import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SplitButtonModule } from 'primeng/splitbutton';

import { DotActionMenuButtonComponent } from './dot-action-menu-button.component';

import { UiDotIconButtonTooltipModule } from '../dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotMenuModule } from '../dot-menu/dot-menu.module';

@NgModule({
    declarations: [DotActionMenuButtonComponent],
    exports: [DotActionMenuButtonComponent],
    imports: [CommonModule, SplitButtonModule, UiDotIconButtonTooltipModule, DotMenuModule]
})
export class DotActionMenuButtonModule {}
