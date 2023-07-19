import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputFieldComponent } from './input-field/input-field.component';

@NgModule({
    imports: [CommonModule, FormsModule],
    declarations: [InputFieldComponent],
    exports: [InputFieldComponent]
})
export class WebComponentsModule {}
