import { ActionMenuButtonComponent } from './action-menu-button.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SplitButtonModule } from 'primeng/splitbutton';
import { DotMenuModule } from '../dot-menu/dot-menu.module';
import { DotIconButtonTooltipModule } from '../dot-icon-button-tooltip/dot-icon-button-tooltip.module';

@NgModule({
    declarations: [ActionMenuButtonComponent],
    exports: [ActionMenuButtonComponent],
    imports: [CommonModule, SplitButtonModule, DotIconButtonTooltipModule, DotMenuModule]
})
export class ActionMenuButtonModule {}
