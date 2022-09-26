import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { CategoriesListComponent } from './categories-list.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { InputNumberModule } from 'primeng/inputnumber';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { FormsModule } from '@angular/forms';

@NgModule({
    declarations: [CategoriesListComponent],
    exports: [CategoriesListComponent],
    imports: [
        CommonModule,
        FormsModule,
        InputNumberModule,
        BreadcrumbModule,
        DotMessagePipeModule,
        DotListingDataTableModule,
        ButtonModule,
        DotBaseTypeSelectorModule,
        CheckboxModule,
        DotEmptyStateModule,
        MenuModule,
        DotPortletBaseModule,
        DotActionMenuButtonModule
    ]
})
export class CategoriesListModule {}
