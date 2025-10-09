import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainerPermissionsComponent } from './dot-container-permissions.component';

import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxModule } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@NgModule({
    declarations: [DotContainerPermissionsComponent],
    exports: [DotContainerPermissionsComponent],
    imports: [CommonModule, DotPortletBoxModule, IframeComponent]
})
export class DotContainerPermissionsModule {}
