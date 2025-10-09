import { NgModule } from '@angular/core';

import { DotDialogComponent } from '@dotcms/ui';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IframeComponent } from '../_common/iframe/iframe-component/iframe.component';

@NgModule({
    imports: [DotDialogComponent, IframeComponent],
    declarations: [DotIframeDialogComponent],
    exports: [DotIframeDialogComponent]
})
export class DotIframeDialogModule {}
