import { DotActionButtonComponent } from './dot-action-button.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SplitButtonModule, ButtonModule, MenuModule } from 'primeng/primeng';

@NgModule({
    declarations: [
        DotActionButtonComponent
    ],
    exports: [
        DotActionButtonComponent
    ],
    imports: [
        CommonModule,
        ButtonModule,
        MenuModule
    ]
})

export class DotActionButtonModule {

}
