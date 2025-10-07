import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotMessagePipe } from '@dotcms/ui';

import { DotContainerCreateRoutingModule } from './dot-container-create-routing.module';
import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotContainerHistoryModule } from './dot-container-history/dot-container-history.module';
import { DotContainerPermissionsModule } from './dot-container-permissions/dot-container-permissions.module';
import { DotContainerPropertiesModule } from './dot-container-properties/dot-container-properties.module';

import { DotGlobalMessageModule } from '../../../view/components/_common/dot-global-message/dot-global-message.module';
import { DotPortletBaseModule } from '../../../view/components/dot-portlet-base/dot-portlet-base.module';

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
