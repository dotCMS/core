import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';
import { DialogModule } from 'primeng/primeng';
import { IFrameModule } from '../_common/iframe';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [CommonModule, DialogModule, IFrameModule, DotIconButtonModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
