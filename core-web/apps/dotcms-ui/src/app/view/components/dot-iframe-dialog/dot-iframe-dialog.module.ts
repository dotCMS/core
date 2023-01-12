import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IFrameModule } from '../_common/iframe';

@NgModule({
    imports: [CommonModule, DotDialogModule, IFrameModule, UiDotIconButtonModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
