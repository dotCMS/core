import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotDialogComponent } from './dot-dialog.component';

import { UiDotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [CommonModule, ButtonModule, CommonModule, DialogModule, UiDotIconButtonModule],
    declarations: [DotDialogComponent],
    exports: [DotDialogComponent]
})
export class DotDialogModule {}
