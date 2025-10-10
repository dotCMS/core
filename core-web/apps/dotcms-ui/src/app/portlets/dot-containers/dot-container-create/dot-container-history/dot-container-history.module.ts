import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainerHistoryComponent } from './dot-container-history.component';

import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';

@NgModule({
    declarations: [DotContainerHistoryComponent],
    exports: [DotContainerHistoryComponent],
    imports: [CommonModule, DotPortletBoxComponent, IframeComponent]
})
export class DotContainerHistoryModule {}
