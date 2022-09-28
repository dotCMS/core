import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerListRoutingModule } from './dot-container-list-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotContainerListComponent } from './dot-container-list.component';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotContainerListResolver } from '@portlets/dot-containers/dot-container-list/dot-container-list-resolver.service';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';

@NgModule({
    declarations: [DotContainerListComponent],
    imports: [
        CommonModule,
        ContainerListRoutingModule,
        DotPortletBaseModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotMessagePipeModule,
        ButtonModule,
        CheckboxModule,
        MenuModule,
        DotEmptyStateModule,
        DotAddToBundleModule
    ],
    providers: [DotContainerListResolver]
})
export class DotContainerListModule {}
