import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerListRoutingModule } from './container-list-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { ContainerListComponent } from './container-list.component';

@NgModule({
    declarations: [ContainerListComponent],
    imports: [CommonModule, ContainerListRoutingModule, DotPortletBaseModule]
})
export class ContainerListModule {}
