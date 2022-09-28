import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerCreateRoutingModule } from './dot-container-create-routing.module';
import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { TabViewModule } from 'primeng/tabview';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotContainerCreateComponent],
    imports: [
        CommonModule,
        ContainerCreateRoutingModule,
        DotPortletBaseModule,
        TabViewModule,
        DotMessagePipeModule
    ]
})
export class ContainerCreateModule {}
