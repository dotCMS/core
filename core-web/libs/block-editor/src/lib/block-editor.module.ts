import { CommonModule } from '@angular/common';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import {
    DotContentSearchService,
    DotLanguagesService,
    DotMessageService,
    DotPropertiesService,
    DotUploadFileService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotAssetSearchComponent, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

//Editor
import { DotBlockEditorComponent } from './components/dot-block-editor/dot-block-editor.component';
import { DotEditorCountBarComponent } from './components/dot-editor-count-bar/dot-editor-count-bar.component';
import {
    AIContentActionsComponent,
    AIContentPromptComponent,
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
import { DotAiService, EditorDirective } from './shared';
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
        AssetFormModule,
        DotFieldRequiredDirective,
        UploadPlaceholderComponent,
        DotMessagePipe,
        ConfirmDialogModule,
        AIImagePromptComponent,
        DotAssetSearchComponent
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
        ConfirmationService,
        DotPropertiesService,
        DotContentSearchService,
        DotLanguagesService,
        {
            provide: APP_INITIALIZER,
            useFactory: initTranslations,
            deps: [DotMessageService],
            multi: true
        }
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
