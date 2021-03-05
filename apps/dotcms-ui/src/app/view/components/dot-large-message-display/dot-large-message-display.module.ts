import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotLargeMessageDisplayComponent],
    imports: [CommonModule, DotDialogModule],
    exports: [DotLargeMessageDisplayComponent]
})
export class DotLargeMessageDisplayModule {}
