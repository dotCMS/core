import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

//Editor
import { DotBlockEditorComponent } from './components/dot-block-editor/dot-block-editor.component';
import { DotEditorCountBarComponent } from './components/dot-editor-count-bar/dot-editor-count-bar.component';
import {
    AIContentPromptComponent,
    AIContentActionsComponent,
    AIImagePromptComponent,
    BubbleFormComponent,
    BubbleLinkFormComponent,
    BubbleMenuButtonComponent,
    BubbleMenuComponent,
    DragHandlerComponent,
    FloatingButtonComponent,
    FormActionsComponent,
    SuggestionPageComponent,
    UploadPlaceholderComponent
} from './extensions';
import { AssetFormModule } from './extensions/asset-form/asset-form.module';
import { ContentletBlockComponent } from './nodes';
import { DotAiService, DotUploadFileService, EditorDirective } from './shared';
import { PrimengModule } from './shared/primeng.module';
import { SharedModule } from './shared/shared.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        SharedModule,
        PrimengModule,
        AssetFormModule,
        DotFieldRequiredDirective,
        UploadPlaceholderComponent,
        DotMessagePipe,
        ConfirmDialogModule,
        AIImagePromptComponent
    ],
    declarations: [
        EditorDirective,
        ContentletBlockComponent,
        DragHandlerComponent,
        BubbleMenuComponent,
        BubbleMenuButtonComponent,
        BubbleLinkFormComponent,
        FormActionsComponent,
        BubbleFormComponent,
        SuggestionPageComponent,
        DotBlockEditorComponent,
        DotEditorCountBarComponent,
        FloatingButtonComponent,
        AIContentPromptComponent,
        AIContentActionsComponent
    ],
    providers: [
        DotUploadFileService,
        LoggerService,
        StringUtils,
        DotAiService,
        ConfirmationService
    ],
    exports: [
        EditorDirective,
        BubbleMenuComponent,
        BubbleLinkFormComponent,
        ReactiveFormsModule,
        SharedModule,
        BubbleFormComponent,
        DotBlockEditorComponent,
        AIContentPromptComponent,
        AIContentActionsComponent
    ]
})
export class BlockEditorModule {}
