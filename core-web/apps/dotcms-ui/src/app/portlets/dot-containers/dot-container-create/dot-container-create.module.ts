import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotContainerCreateRoutingModule } from './dot-container-create-routing.module';
import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { TabViewModule } from 'primeng/tabview';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotContainerPropertiesModule } from '@portlets/dot-containers/dot-container-create/dot-container-properties/dot-container-properties.module';
import { DotContainerPermissionsModule } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-permissions/dot-container-permissions.module';
import { DotContainerHistoryModule } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-history/dot-container-history.module';
import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';

@NgModule({
    declarations: [DotContainerCreateComponent],
    imports: [
        CommonModule,
        DotContainerCreateRoutingModule,
        DotPortletBaseModule,
        TabViewModule,
        DotMessagePipeModule,
        DotContainerPropertiesModule,
        DotContainerPermissionsModule,
        DotContainerHistoryModule,
        DotGlobalMessageModule
    ]
})
export class DotContainerCreateModule {}
