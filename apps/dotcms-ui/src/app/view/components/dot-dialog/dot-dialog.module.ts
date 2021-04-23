import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotDialogComponent } from './dot-dialog.component';
import { UiDotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

@NgModule({
    imports: [CommonModule, ButtonModule, CommonModule, DialogModule, UiDotIconButtonModule],
    declarations: [DotDialogComponent],
    exports: [DotDialogComponent]
})
export class DotDialogModule {}
