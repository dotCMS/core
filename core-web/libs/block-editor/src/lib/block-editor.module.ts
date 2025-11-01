import { TiptapBubbleMenuDirective } from 'ngx-tiptap';

import { CommonModule } from '@angular/common';
import { NgModule, inject, provideAppInitializer } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { PaginatorModule } from 'primeng/paginator';

import {
    DotContentSearchService,
    DotLanguagesService,
    DotMessageService,
    DotPropertiesService,
    DotUploadFileService,
    DotAiService,
    DotWorkflowActionsFireService,
    DotContentTypeService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import {
    DotAssetSearchComponent,
    DotFieldRequiredDirective,
    DotMessagePipe,
    DotSpinnerComponent
} from '@dotcms/ui';

//Editor
import { DotBlockEditorComponent } from './components/dot-block-editor/dot-block-editor.component';
import { DotEditorCountBarComponent } from './components/dot-editor-count-bar/dot-editor-count-bar.component';
import { DragHandleDirective } from './directive/drag-handle.directive';
import { DotAddButtonComponent } from './elements/dot-add-button/dot-add-button.component';
import { DotBubbleMenuComponent } from './elements/dot-bubble-menu/dot-bubble-menu.component';
import { DotContextMenuComponent } from './elements/dot-context-menu/dot-context-menu.component';
import {
    BubbleFormComponent,
    FloatingButtonComponent,
    UploadPlaceholderComponent
} from './extensions';
import { AssetFormModule } from './extensions/asset-form/asset-form.module';
import { ContentletBlockComponent } from './nodes';
import { EditorDirective } from './shared';
import { PrimengModule } from './shared/primeng.module';
import { SharedModule } from './shared/shared.module';

const initTranslations = (dotMessageService: DotMessageService) => {
    return () => dotMessageService.init();
};

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        SharedModule,
        PrimengModule,
        DynamicDialogModule,
        AssetFormModule,
        DotFieldRequiredDirective,
        UploadPlaceholderComponent,
        DotMessagePipe,
        ConfirmDialogModule,
        DotAssetSearchComponent,
        DialogModule,
        InputTextareaModule,
        PaginatorModule,
        DotSpinnerComponent,
        DotBubbleMenuComponent,
        TiptapBubbleMenuDirective,
        DragHandleDirective,
        DotContextMenuComponent,
        DotAddButtonComponent
    ],
    declarations: [
        EditorDirective,
        ContentletBlockComponent,
        BubbleFormComponent,
        DotBlockEditorComponent,
        DotEditorCountBarComponent,
        FloatingButtonComponent
    ],
    providers: [
        DotUploadFileService,
        LoggerService,
        StringUtils,
        DotAiService,
        ConfirmationService,
        DotPropertiesService,
        DotContentSearchService,
        DotLanguagesService,
        DotContentTypeService,
        DotWorkflowActionsFireService,
        provideAppInitializer(() => {
            const initializerFn = initTranslations(inject(DotMessageService));
            return initializerFn();
        })
    ],
    exports: [
        EditorDirective,
        DotBubbleMenuComponent,
        ReactiveFormsModule,
        SharedModule,
        BubbleFormComponent,
        DotBlockEditorComponent,
        DotSpinnerComponent,
        DragHandleDirective
    ]
})
export class BlockEditorModule {}
