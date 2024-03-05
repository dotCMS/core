import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';

import { DotDialogModule } from '../dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotLargeMessageDisplayComponent],
    imports: [CommonModule, DotDialogModule],
    exports: [DotLargeMessageDisplayComponent]
})
export class DotLargeMessageDisplayModule {}
