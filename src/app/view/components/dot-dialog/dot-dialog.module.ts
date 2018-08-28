import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule, ButtonModule } from 'primeng/primeng';
import { DotDialogComponent } from './dot-dialog.component';
import { DotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [CommonModule, ButtonModule, CommonModule, DialogModule, DotIconButtonModule],
    declarations: [DotDialogComponent],
    exports: [DotDialogComponent]
})
export class DotDialogModule {}
