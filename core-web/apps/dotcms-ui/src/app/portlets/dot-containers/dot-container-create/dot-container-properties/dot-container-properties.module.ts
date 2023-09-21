import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { SharedModule } from 'primeng/api';
import { CardModule } from 'primeng/card';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { TabViewModule } from 'primeng/tabview';

import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotContentEditorModule } from '@portlets/dot-containers/dot-container-create/dot-container-code/dot-container-code.module';
import { DotContainerPropertiesComponent } from '@portlets/dot-containers/dot-container-create/dot-container-properties/dot-container-properties.component';
import { DotLoopEditorModule } from '@portlets/dot-containers/dot-container-create/dot-loop-editor/dot-loop-editor.module';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';

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
        DotMessagePipe,
        DotLoopEditorModule,
        DotContentEditorModule,
        DotApiLinkModule,
        DotAutofocusModule,
        DotFieldRequiredDirective
    ],
    providers: [DotContainersService]
})
export class DotContainerPropertiesModule {}
