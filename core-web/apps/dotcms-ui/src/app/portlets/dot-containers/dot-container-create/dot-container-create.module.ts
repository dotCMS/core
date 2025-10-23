import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { TabViewModule } from 'primeng/tabview';

import { DotMessagePipe } from '@dotcms/ui';

import { DotContainerCreateComponent } from './dot-container-create.component';
import { dotContainerCreateRoutes } from './dot-container-create.routes';
import { DotContainerHistoryComponent } from './dot-container-history/dot-container-history.component';
import { DotContainerPermissionsComponent } from './dot-container-permissions/dot-container-permissions.component';
import { DotContainerPropertiesComponent } from './dot-container-properties/dot-container-properties.component';

import { DotGlobalMessageComponent } from '../../../view/components/_common/dot-global-message/dot-global-message.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@NgModule({
    declarations: [DotContainerCreateComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(dotContainerCreateRoutes),
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
