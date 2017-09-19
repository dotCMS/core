import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {DotLoadingIndicatorComponent} from './dot-loading-indicator.component';

@NgModule({
    imports: [
        CommonModule
    ],
    declarations: [
        DotLoadingIndicatorComponent
    ],
    exports: [
        DotLoadingIndicatorComponent
    ]
})
export class DotLoadingIndicatorModule { }
