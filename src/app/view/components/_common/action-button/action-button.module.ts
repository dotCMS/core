import { ActionButtonComponent } from './action-button.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SplitButtonModule } from 'primeng/primeng';

@NgModule({
    declarations: [
        ActionButtonComponent
    ],
    exports: [
        ActionButtonComponent
    ],
    imports: [
        CommonModule,
        SplitButtonModule
    ]
})

export class ActionButtonModule {

}
