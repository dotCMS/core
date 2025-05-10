import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { DotAlertConfirmService } from '@dotcms/data-access';

import { DotAlertConfirmComponent } from './dot-alert-confirm';

@NgModule({
    imports: [CommonModule, ConfirmDialogModule, DialogModule],
    declarations: [DotAlertConfirmComponent],
    exports: [DotAlertConfirmComponent],
    providers: [DotAlertConfirmService]
})
export class DotAlertConfirmModule {}
