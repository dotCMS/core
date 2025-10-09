import { NgModule } from '@angular/core';

import { DotDialogComponent } from '@dotcms/ui';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IFrameModule } from '../_common/iframe';

@NgModule({
    imports: [DotDialogComponent, IFrameModule],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
