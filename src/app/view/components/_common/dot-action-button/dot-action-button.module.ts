import { DotActionButtonComponent } from './dot-action-button.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SplitButtonModule } from 'primeng/primeng';
import { IconButtonTooltipModule } from '../icon-button-tooltip/icon-button-tooltip.module';

@NgModule({
    declarations: [
        DotActionButtonComponent
    ],
    exports: [
        DotActionButtonComponent
    ],
    imports: [
        CommonModule,
        SplitButtonModule,
        IconButtonTooltipModule
    ]
})

export class DotActionButtonModule {

}
