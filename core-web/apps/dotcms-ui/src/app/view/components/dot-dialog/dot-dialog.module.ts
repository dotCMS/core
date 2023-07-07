import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { UiDotIconButtonModule } from '@dotcms/ui';

import { DotDialogComponent } from './dot-dialog.component';

@NgModule({
    imports: [CommonModule, ButtonModule, CommonModule, DialogModule, UiDotIconButtonModule],
    declarations: [DotDialogComponent],
    exports: [DotDialogComponent]
})
export class DotDialogModule {}
