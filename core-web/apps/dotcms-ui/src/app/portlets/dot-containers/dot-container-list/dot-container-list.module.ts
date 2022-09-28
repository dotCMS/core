import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerListRoutingModule } from './dot-container-list-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotContainerListComponent } from './dot-container-list.component';

@NgModule({
    declarations: [DotContainerListComponent],
    imports: [CommonModule, ContainerListRoutingModule, DotPortletBaseModule]
})
export class DotContainerListModule {}
