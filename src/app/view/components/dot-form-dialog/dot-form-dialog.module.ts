import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DotFormDialogComponent } from './dot-form-dialog.component';
import { FocusTrapModule } from 'primeng/focustrap';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotFormDialogComponent],
    exports: [DotFormDialogComponent],
    imports: [CommonModule, ButtonModule, FocusTrapModule, DotMessagePipeModule]
})
export class DotFormDialogModule {}
