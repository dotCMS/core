import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotDialogMessageComponent } from './dot-dialog-message.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotDialogMessageComponent],
    imports: [CommonModule, DotDialogModule],
    exports: [DotDialogMessageComponent]
})
export class DotDialogMessageModule {}
