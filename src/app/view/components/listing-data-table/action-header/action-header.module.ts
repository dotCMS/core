import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotActionButtonModule } from '../../_common/dot-action-button/dot-action-button.module';
import { ActionHeaderComponent } from './action-header';
import { SplitButtonModule } from 'primeng/primeng';

@NgModule({
    bootstrap: [],
    declarations: [
        ActionHeaderComponent
    ],
    exports: [
        ActionHeaderComponent
    ],
    imports: [
        CommonModule,
        DotActionButtonModule,
        SplitButtonModule
    ],
    providers: []
})
export class ActionHeaderModule {}
