import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';

@NgModule({
    declarations: [DotLargeMessageDisplayComponent],
    imports: [CommonModule, DotDialogModule],
    exports: [DotLargeMessageDisplayComponent]
})
export class DotLargeMessageDisplayModule {}
