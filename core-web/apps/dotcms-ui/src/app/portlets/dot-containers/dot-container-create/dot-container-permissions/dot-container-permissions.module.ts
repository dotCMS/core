import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { IFrameModule } from '@components/_common/iframe';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

import { DotContainerPermissionsComponent } from './dot-container-permissions.component';

@NgModule({
    declarations: [DotContainerPermissionsComponent],
    exports: [DotContainerPermissionsComponent],
    imports: [CommonModule, DotPortletBoxModule, IFrameModule]
})
export class DotContainerPermissionsModule {}
