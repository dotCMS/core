import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerCreateRoutingModule } from './container-create-routing.module';
import { ContainerCreateComponent } from './container-create.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    declarations: [ContainerCreateComponent],
    imports: [CommonModule, ContainerCreateRoutingModule, DotPortletBaseModule]
})
export class ContainerCreateModule {}
