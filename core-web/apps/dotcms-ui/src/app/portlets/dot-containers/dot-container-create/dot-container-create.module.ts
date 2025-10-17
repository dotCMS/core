import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotMessagePipe } from '@dotcms/ui';

import { DotContainerCreateRoutingModule } from './dot-container-create-routing.module';
import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotContainerHistoryComponent } from './dot-container-history/dot-container-history.component';
import { DotContainerPermissionsComponent } from './dot-container-permissions/dot-container-permissions.component';
import { DotContainerPropertiesComponent } from './dot-container-properties/dot-container-properties.component';

import { DotGlobalMessageComponent } from '../../../view/components/_common/dot-global-message/dot-global-message.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@NgModule({
    declarations: [DotContainerCreateComponent],
    imports: [
        CommonModule,
        DotContainerCreateRoutingModule,
        DotPortletBaseComponent,
        TabViewModule,
        DotMessagePipe,
        DotContainerPropertiesComponent,
        DotContainerPermissionsComponent,
        DotContainerHistoryComponent,
        DotGlobalMessageComponent
    ]
})
export class DotContainerCreateModule {}
