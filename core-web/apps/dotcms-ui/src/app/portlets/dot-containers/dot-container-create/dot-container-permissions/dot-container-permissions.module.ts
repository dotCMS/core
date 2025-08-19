import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainerPermissionsComponent } from './dot-container-permissions.component';

import { IFrameModule } from '../../../../view/components/_common/iframe/iframe.module';
import { DotPortletBoxModule } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@NgModule({
    declarations: [DotContainerPermissionsComponent],
    exports: [DotContainerPermissionsComponent],
    imports: [CommonModule, DotPortletBoxModule, IFrameModule]
})
export class DotContainerPermissionsModule {}
