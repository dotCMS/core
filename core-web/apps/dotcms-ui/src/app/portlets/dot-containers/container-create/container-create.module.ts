import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerCreateRoutingModule } from './container-create-routing.module';
import { ContainerCreateComponent } from './container-create.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { TabViewModule } from 'primeng/tabview';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [ContainerCreateComponent],
    imports: [
        CommonModule,
        ContainerCreateRoutingModule,
        DotPortletBaseModule,
        TabViewModule,
        DotMessagePipeModule
    ]
})
export class ContainerCreateModule {}
