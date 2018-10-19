import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';
import { IFrameModule } from '../_common/iframe';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@NgModule({
    imports: [CommonModule, DotDialogModule, IFrameModule, DotIconButtonModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
