import { NgModule } from '@angular/core';
import { DotContainerPropertiesComponent } from '@portlets/dot-containers/dot-container-create/dot-container-properties/dot-container-properties.component';
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
import { DotLoopEditorModule } from '@portlets/dot-containers/dot-container-create/dot-loop-editor/dot-loop-editor.module';
import { DotContentEditorModule } from '@portlets/dot-containers/dot-container-create/dot-container-code/dot-container-code.module';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';

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
        DotMessagePipeModule,
        DotLoopEditorModule,
        DotContentEditorModule,
        DotApiLinkModule,
        DotAutofocusModule
    ],
    providers: [DotContainersService]
})
export class DotContainerPropertiesModule {}
