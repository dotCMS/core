import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotContentTypeSelectorModule } from '@components/dot-content-type-selector';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotRelativeDatePipe } from '@dotcms/app/view/pipes/dot-relative-date/dot-relative-date.pipe';
import { DotSiteBrowserService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotContainerListResolver } from '@portlets/dot-containers/container-list/dot-container-list-resolver.service';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';

import { ContainerListRoutingModule } from './container-list-routing.module';
import { ContainerListComponent } from './container-list.component';

@NgModule({
    declarations: [ContainerListComponent],
    imports: [
        CommonModule,
        ContainerListRoutingModule,
        DotPortletBaseModule,
        DotListingDataTableModule,
        DotContentTypeSelectorModule,
        DotMessagePipe,
        ButtonModule,
        CheckboxModule,
        MenuModule,
        DotEmptyStateModule,
        DotAddToBundleModule,
        DotActionMenuButtonModule,
        DotRelativeDatePipe
    ],
    providers: [
        DotContainerListResolver,
        DotSiteBrowserService,
        DotContainersService,
        DialogService
    ]
})
export class ContainerListModule {}
