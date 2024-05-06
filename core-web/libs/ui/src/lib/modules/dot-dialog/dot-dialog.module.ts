import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotDialogComponent } from './dot-dialog.component';

@NgModule({
    imports: [CommonModule, ButtonModule, CommonModule, DialogModule],
    declarations: [DotDialogComponent],
    exports: [DotDialogComponent]
})
export class DotDialogModule {}
