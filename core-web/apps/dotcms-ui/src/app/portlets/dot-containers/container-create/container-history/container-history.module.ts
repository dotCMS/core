import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerHistoryComponent } from './container-history.component';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { IFrameModule } from '@components/_common/iframe';

@NgModule({
    declarations: [ContainerHistoryComponent],
    exports: [ContainerHistoryComponent],
    imports: [CommonModule, DotPortletBoxModule, IFrameModule]
})
export class ContainerHistoryModule {}
