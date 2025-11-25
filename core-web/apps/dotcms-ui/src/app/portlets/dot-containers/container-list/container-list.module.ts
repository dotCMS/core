import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';

import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotContentTypeSelectorModule } from '@components/dot-content-type-selector';
import { ActionHeaderModule } from '@components/dot-listing-data-table/action-header/action-header.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotSiteBrowserService } from '@dotcms/data-access';
import {
    DotActionMenuButtonComponent,
    DotAddToBundleComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';
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
        TableModule,
        DotContentTypeSelectorModule,
        DotMessagePipe,
        ButtonModule,
        CheckboxModule,
        MenuModule,
        DotEmptyStateModule,
        DotAddToBundleComponent,
        DotActionMenuButtonComponent,
        DotRelativeDatePipe,
        ActionHeaderModule,
        InputTextModule
    ],
    providers: [
        DotContainerListResolver,
        DotSiteBrowserService,
        DotContainersService,
        DialogService
    ]
})
export class ContainerListModule {}
