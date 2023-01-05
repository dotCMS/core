import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { IFrameModule } from '../_common/iframe';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

@NgModule({
    imports: [CommonModule, DotDialogModule, IFrameModule, UiDotIconButtonModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
