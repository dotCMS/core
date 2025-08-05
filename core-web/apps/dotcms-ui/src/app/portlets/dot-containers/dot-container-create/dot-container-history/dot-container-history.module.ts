import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainerHistoryComponent } from './dot-container-history.component';

import { IFrameModule } from '../../../../view/components/_common/iframe/iframe.module';
import { DotPortletBoxModule } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@NgModule({
    declarations: [DotContainerHistoryComponent],
    exports: [DotContainerHistoryComponent],
    imports: [CommonModule, DotPortletBoxModule, IFrameModule]
})
export class DotContainerHistoryModule {}
