import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { IFrameModule } from '@components/_common/iframe';
import { DotContainerHistoryComponent } from './dot-container-history.component';

@NgModule({
    declarations: [DotContainerHistoryComponent],
    exports: [DotContainerHistoryComponent],
    imports: [CommonModule, DotPortletBoxModule, IFrameModule]
})
export class DotContainerHistoryModule {}
