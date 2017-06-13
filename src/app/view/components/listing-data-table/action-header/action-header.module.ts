import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActionButtonModule } from '../../_common/action-button/action-button.module';
import { ActionHeaderComponent } from './action-header';
import { ConfirmDialogModule, SplitButtonModule } from 'primeng/primeng';

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
        ActionButtonModule,
        SplitButtonModule,
        ConfirmDialogModule
    ],
    providers: []
})
export class ActionHeaderModule {}