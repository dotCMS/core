import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotSpinnerComponent } from '@components/_common/dot-spinner/dot-spinner.component';

@NgModule({
    declarations: [DotSpinnerComponent],
    imports: [CommonModule],
    exports: [DotSpinnerComponent],
    providers: []
})
export class DotSpinnerModule {}
