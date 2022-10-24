import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotCategoriesTableComponent } from './dot-categories-table.component';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { PaginatorModule } from 'primeng/paginator';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { FormsModule } from '@angular/forms';
import { InplaceModule } from 'primeng/inplace';
import { InputNumberModule } from 'primeng/inputnumber';

@NgModule({
    declarations: [DotCategoriesTableComponent],
    exports: [DotCategoriesTableComponent],
    imports: [
        CommonModule,
        DotPortletBaseModule,
        MenuModule,
        ButtonModule,
        InputTextModule,
        TableModule,
        PaginatorModule,
        InplaceModule,
        InputNumberModule,
        DotActionMenuButtonModule,
        FormsModule,
        DotMessagePipeModule
    ]
})
export class DotCategoriesTableModule {}
