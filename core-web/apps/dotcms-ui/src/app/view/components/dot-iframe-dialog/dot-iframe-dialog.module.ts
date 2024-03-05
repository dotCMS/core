import { NgModule } from '@angular/core';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IFrameModule } from '../_common/iframe';
import { DotDialogModule } from '../dot-dialog/dot-dialog.module';

@NgModule({
    imports: [DotDialogModule, IFrameModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
