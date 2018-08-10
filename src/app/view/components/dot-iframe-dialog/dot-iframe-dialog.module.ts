import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';
import { DialogModule } from 'primeng/primeng';
import { IFrameModule } from '../_common/iframe';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, DialogModule, IFrameModule, DotIconModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
