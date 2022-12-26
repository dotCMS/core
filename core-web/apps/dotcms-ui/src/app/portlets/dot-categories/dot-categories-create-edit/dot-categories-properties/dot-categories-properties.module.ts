import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InplaceModule } from 'primeng/inplace';
import { SharedModule } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';
import { ReactiveFormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { MenuModule } from 'primeng/menu';

import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotCategoriesPropertiesComponent } from './dot-categories-properties.component';

@NgModule({
    declarations: [DotCategoriesPropertiesComponent],
    exports: [DotCategoriesPropertiesComponent],
    imports: [
        CommonModule,
        InplaceModule,
        SharedModule,
        InputTextModule,
        DotPortletBaseModule,
        CardModule,
        DotTextareaContentModule,
        ReactiveFormsModule,
        TabViewModule,
        MenuModule,
        DotMessagePipeModule,
        DotApiLinkModule
    ],
    providers: []
})
export class DotCategoriesPropertiesModule {}
