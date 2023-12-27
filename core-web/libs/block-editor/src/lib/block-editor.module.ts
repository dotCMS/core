import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { APP_INITIALIZER, ErrorHandler, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { DotMessageService } from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

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
import { DotAiService, DotUploadFileService, EditorDirective } from './shared';
import { HttpErrorInterceptor } from './shared/interceptors/http-error-interceptor';
import { PrimengModule } from './shared/primeng.module';
import { BlockEditorErrorHandlerService } from './shared/services/block-editor-error-handler/block-editor-error-handler.service';
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
        AIImagePromptComponent,
        DotMessagePipe,
        ConfirmDialogModule
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
        BlockEditorErrorHandlerService,
        {
            provide: APP_INITIALIZER,
            useFactory: initTranslations,
            deps: [DotMessageService],
            multi: true
        },
        {
            provide: ErrorHandler,
            useClass: BlockEditorErrorHandlerService
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: HttpErrorInterceptor,
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
        AIContentActionsComponent,
        AIImagePromptComponent
    ]
})
export class BlockEditorModule {}
