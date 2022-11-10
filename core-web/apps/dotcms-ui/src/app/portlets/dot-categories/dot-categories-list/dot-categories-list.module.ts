import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCategoriesListRoutingModule } from './dot-categories-list-routing.module';
import { DotCategoriesListComponent } from './dot-categories-list.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { PaginatorModule } from 'primeng/paginator';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { InplaceModule } from 'primeng/inplace';
import { InputNumberModule } from 'primeng/inputnumber';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { CheckboxModule } from 'primeng/checkbox';
import { DotCategoriesService } from '@dotcms/app/api/services/dot-categories/dot-categories.service';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';

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
        DotActionMenuButtonModule,
        DotMessagePipeModule,
        CheckboxModule,
        BreadcrumbModule,
        DotEmptyStateModule
    ],
    providers: [DotCategoriesService]
})
export class DotCategoriesListingModule {}
