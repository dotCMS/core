import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InplaceModule } from 'primeng/inplace';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';

import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotCategoriesService } from '@dotcms/app/api/services/dot-categories/dot-categories.service';
import { DotActionMenuButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotCategoriesListRoutingModule } from './dot-categories-list-routing.module';
import { DotCategoriesListComponent } from './dot-categories-list.component';

@NgModule({
    declarations: [DotCategoriesListComponent],
    imports: [
        CommonModule,
        DotCategoriesListRoutingModule,
        DotPortletBaseModule,
        MenuModule,
        ButtonModule,
        InputTextModule,
        TableModule,
        PaginatorModule,
        InplaceModule,
        InputNumberModule,
        DotActionMenuButtonComponent,
        DotMessagePipe,
        CheckboxModule,
        BreadcrumbModule,
        DotEmptyStateModule
    ],
    providers: [DotCategoriesService]
})
export class DotCategoriesListingModule {}
