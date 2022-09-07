import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerCreateRoutingModule } from './container-create-routing.module';
import { ContainerCreateComponent } from './container-create.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { TabViewModule } from 'primeng/tabview';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotContainerPropertiesModule } from '@portlets/dot-containers/container-create/dot-container-properties/dot-container-properties.module';

@NgModule({
    declarations: [ContainerCreateComponent],
    imports: [
        CommonModule,
        ContainerCreateRoutingModule,
        DotPortletBaseModule,
        TabViewModule,
        DotMessagePipeModule,
        DotContainerPropertiesModule
    ]
})
export class ContainerCreateModule {}
