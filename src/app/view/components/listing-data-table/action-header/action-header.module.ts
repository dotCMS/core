import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActionButtonModule } from '../../_common/action-button/action-button.module';
import { ActionHeaderComponent } from './action-header';
import { SplitButtonModule } from 'primeng/primeng';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation/';

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
        SplitButtonModule
    ],
    providers: []
})
export class ActionHeaderModule {}