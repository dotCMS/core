import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotContainerPermissionsComponent } from './dot-container-permissions.component';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { IFrameModule } from '@components/_common/iframe';

@NgModule({
    declarations: [DotContainerPermissionsComponent],
    exports: [DotContainerPermissionsComponent],
    imports: [CommonModule, DotPortletBoxModule, IFrameModule]
})
export class DotContainerPermissionsModule {}
