import { NgModule } from '@angular/core';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IFrameModule } from '../_common/iframe';

@NgModule({
    imports: [CommonModule, DotDialogModule, IFrameModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
