import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerCreateRoutingModule } from './container-create-routing.module';
import { ContainerCreateComponent } from './container-create.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { TabViewModule } from 'primeng/tabview';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotContainerPropertiesModule } from '@portlets/dot-containers/container-create/dot-container-properties/dot-container-properties.module';
import { ContainerPermissionsModule } from '@dotcms/app/portlets/dot-containers/container-create/container-permissions/container-permissions.module';
import { ContainerHistoryModule } from '@dotcms/app/portlets/dot-containers/container-create/container-history/container-history.module';

@NgModule({
    declarations: [ContainerCreateComponent],
    imports: [
        CommonModule,
        ContainerCreateRoutingModule,
        DotPortletBaseModule,
        TabViewModule,
        DotMessagePipeModule,
        DotContainerPropertiesModule,
        DotMessagePipeModule,
        ContainerPermissionsModule,
        ContainerHistoryModule
    ]
})
export class ContainerCreateModule {}
