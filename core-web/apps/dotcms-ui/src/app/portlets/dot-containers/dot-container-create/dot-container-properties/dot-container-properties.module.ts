import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { SharedModule } from 'primeng/api';
import { CardModule } from 'primeng/card';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { TabViewModule } from 'primeng/tabview';

import {
    DotApiLinkComponent,
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotMessagePipe
} from '@dotcms/ui';

import { DotContainerPropertiesComponent } from './dot-container-properties.component';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';
import { DotTextareaContentComponent } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';
import { DotPortletBaseModule } from '../../../../view/components/dot-portlet-base/dot-portlet-base.module';
import { DotContentEditorModule } from '../dot-container-code/dot-container-code.module';
import { DotLoopEditorModule } from '../dot-loop-editor/dot-loop-editor.module';

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
        DotTextareaContentComponent,
        ReactiveFormsModule,
        TabViewModule,
        MenuModule,
        DotMessagePipe,
        DotLoopEditorModule,
        DotContentEditorModule,
        DotApiLinkComponent,
        DotAutofocusDirective,
        DotFieldRequiredDirective
    ],
    providers: [DotContainersService]
})
export class DotContainerPropertiesModule {}
