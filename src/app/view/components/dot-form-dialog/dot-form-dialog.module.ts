import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DotFormDialogComponent } from './dot-form-dialog.component';
import { FocusTrapModule } from 'primeng/focustrap';

@NgModule({
    declarations: [DotFormDialogComponent],
    exports: [DotFormDialogComponent],
    imports: [CommonModule, ButtonModule, FocusTrapModule]
})
export class DotFormDialogModule {}
