import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotFieldRequiredDirective } from '@dotcms/ui';

//Editor
import { DotBlockEditorComponent } from './components/dot-block-editor/dot-block-editor.component';
import { DotEditorCountBarComponent } from './components/dot-editor-count-bar/dot-editor-count-bar.component';
import {
    AIContentActionsComponent,
    AIContentPromptComponent,
    AIImagePromptComponent,
    BubbleLinkFormComponent,
    BubbleMenuButtonComponent,
    BubbleMenuComponent,
    DragHandlerComponent,
    FormActionsComponent,
    SuggestionPageComponent,
    UploadPlaceholderComponent
} from './extensions';
import { AssetFormModule } from './extensions/asset-form/asset-form.module';
import { BubbleFormComponent } from './extensions/bubble-form/bubble-form.component';
import { FloatingButtonComponent } from './extensions/floating-button/floating-button.component';
import { ContentletBlockComponent } from './nodes';
import { DotAiService, DotUploadFileService } from './shared';
import { EditorDirective } from './shared/directives';
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
    providers: [DotUploadFileService, LoggerService, StringUtils, DotAiService],
    exports: [
        EditorDirective,
        BubbleMenuComponent,
        BubbleLinkFormComponent,
        ReactiveFormsModule,
        SharedModule,
        BubbleFormComponent,
        DotBlockEditorComponent,
        AIContentPromptComponent,
        AIContentActionsComponent,
        AIImagePromptComponent
    ]
})
export class BlockEditorModule {}
