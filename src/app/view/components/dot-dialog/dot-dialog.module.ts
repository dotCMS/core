import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule, ButtonModule } from 'primeng/primeng';
import { DotDialogComponent } from './dot-dialog.component';

@NgModule({
    imports: [CommonModule, ButtonModule, CommonModule, DialogModule],
    declarations: [DotDialogComponent],
    exports: [DotDialogComponent]
})
export class DotDialogModule {}
