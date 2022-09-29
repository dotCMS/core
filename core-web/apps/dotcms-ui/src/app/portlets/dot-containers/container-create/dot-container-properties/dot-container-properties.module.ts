import { NgModule } from '@angular/core';
import { DotContainerPropertiesComponent } from '@portlets/dot-containers/container-create/dot-container-properties/dot-container-properties.component';
import { CommonModule } from '@angular/common';
import { InplaceModule } from 'primeng/inplace';
import { SharedModule } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { CardModule } from 'primeng/card';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { ReactiveFormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { MenuModule } from 'primeng/menu';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
@NgModule({
    declarations: [DotContainerPropertiesComponent],
    exports: [DotContainerPropertiesComponent],
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
        DotMessagePipeModule
    ]
})
export class DotContainerPropertiesModule {}
