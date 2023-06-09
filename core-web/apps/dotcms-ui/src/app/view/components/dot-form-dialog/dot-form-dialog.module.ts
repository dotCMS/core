import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { FocusTrapModule } from 'primeng/focustrap';

import { DotMessagePipeModule } from '@dotcms/ui';

import { DotFormDialogComponent } from './dot-form-dialog.component';

@NgModule({
    declarations: [DotFormDialogComponent],
    exports: [DotFormDialogComponent],
    imports: [CommonModule, ButtonModule, FocusTrapModule, DotMessagePipeModule]
})
export class DotFormDialogModule {}
