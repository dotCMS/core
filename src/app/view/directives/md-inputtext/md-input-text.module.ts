import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MaterialDesignTextfieldDirective } from './md-input-text.directive';

@NgModule({
    imports: [
        CommonModule
    ],
    declarations: [
      MaterialDesignTextfieldDirective
    ],
    exports: [
        MaterialDesignTextfieldDirective
    ]
})
export class MdInputTextModule { }
