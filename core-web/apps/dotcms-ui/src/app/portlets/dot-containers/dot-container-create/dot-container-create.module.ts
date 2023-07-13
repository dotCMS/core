import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotContainerHistoryModule } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-history/dot-container-history.module';
import { DotContainerPermissionsModule } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-permissions/dot-container-permissions.module';
import { DotMessagePipe } from '@dotcms/ui';
import { DotContainerPropertiesModule } from '@portlets/dot-containers/dot-container-create/dot-container-properties/dot-container-properties.module';

import { DotContainerCreateRoutingModule } from './dot-container-create-routing.module';
import { DotContainerCreateComponent } from './dot-container-create.component';

@NgModule({
    declarations: [DotContainerCreateComponent],
    imports: [
        CommonModule,
        DotContainerCreateRoutingModule,
        DotPortletBaseModule,
        TabViewModule,
        DotMessagePipe,
        DotContainerPropertiesModule,
        DotContainerPermissionsModule,
        DotContainerHistoryModule,
        DotGlobalMessageModule
    ]
})
export class DotContainerCreateModule {}
