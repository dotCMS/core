import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContainerListRoutingModule } from './container-list-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { ContainerListComponent } from './container-list.component';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotContainerListResolver } from '@portlets/dot-containers/container-list/dot-container-list-resolver.service';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotSiteBrowserService } from '@dotcms/data-access';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';

@NgModule({
    declarations: [ContainerListComponent],
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
        DotAddToBundleModule,
        DotActionMenuButtonModule
    ],
    providers: [
        DotContainerListResolver,
        DotSiteBrowserService,
        DotContainersService,
        DialogService
    ]
})
export class ContainerListModule {}
