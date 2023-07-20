import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { InputFieldComponent } from './input-field/input-field.component';

@NgModule({
    imports: [
        CommonModule,
        BrowserAnimationsModule,
        FormsModule,
        InputTextModule,
        DropdownModule,
        ButtonModule
    ],
    declarations: [InputFieldComponent],
    exports: [InputFieldComponent]
})
export class ContenttypeFieldsModule {}
