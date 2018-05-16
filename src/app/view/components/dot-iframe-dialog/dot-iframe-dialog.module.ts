import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';
import { DialogModule } from 'primeng/primeng';
import { IFrameModule } from '../_common/iframe';

@NgModule({
    imports: [CommonModule, DialogModule, IFrameModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
