import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotSpinnerComponent } from './dot-spinner.component';

@NgModule({
    declarations: [DotSpinnerComponent],
    imports: [CommonModule],
    exports: [DotSpinnerComponent],
    providers: []
})
export class DotSpinnerModule {}
