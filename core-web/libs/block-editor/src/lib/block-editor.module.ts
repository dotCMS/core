import { TiptapBubbleMenuDirective } from 'ngx-tiptap';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { NgModule, inject, provideAppInitializer } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { Checkbox } from 'primeng/checkbox';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { DynamicDialog } from 'primeng/dynamicdialog';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';

import { catchError, switchMap, take } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotLanguagesService,
    DotLocalstorageService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowActionsFireService,
    DotContentTypeService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoggerService, StringUtils } from '@dotcms/dotcms-js';
import {
    DotAssetSearchComponent,
    DotFieldRequiredDirective,
    DotMessagePipe,
    DotSpinnerComponent,
    DotContentletStatusBadgeComponent,
    DotContentThumbnailComponent
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
import { SharedModule } from './shared/shared.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        SharedModule,
        DynamicDialog,
        AssetFormModule,
        DotFieldRequiredDirective,
        UploadPlaceholderComponent,
        DotMessagePipe,
        ConfirmDialog,
        DotAssetSearchComponent,
        Dialog,
        Textarea,
        Card,
        Checkbox,
        InputText,
        Button,
        DotSpinnerComponent,
        DotBubbleMenuComponent,
        TiptapBubbleMenuDirective,
        DragHandleDirective,
        DotContextMenuComponent,
        DotAddButtonComponent,
        DotContentletStatusBadgeComponent,
        DotContentThumbnailComponent
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
        LoggerService,
        StringUtils,
        ConfirmationService,
        DotPropertiesService,
        DotContentSearchService,
        DotLanguagesService,
        DotContentTypeService,
        DotWorkflowActionsFireService,
        provideAppInitializer(() => {
            const messageService = inject(DotMessageService);
            const configService = inject(DotcmsConfigService);
            const storage = inject(DotLocalstorageService);
            const logger = inject(LoggerService);
            let language = 'default';
            try {
                const cachedLanguage = storage.getItem<string>('dotMessagesKeys-lang');
                if (
                    typeof cachedLanguage === 'string' &&
                    /^[a-zA-Z][a-zA-Z0-9_-]*$/.test(cachedLanguage)
                ) {
                    language = cachedLanguage;
                }
            } catch (error) {
                logger.warn('Unable to read cached translation locale; using default.', error);
            }

            return configService.getConfig().pipe(
                take(1),
                catchError((error) => {
                    logger.warn(
                        'Unable to load configuration; fetching uncached translations.',
                        error
                    );

                    return of(null);
                }),
                switchMap((config) =>
                    messageService.init({
                        language,
                        languageBundleVersion: config?.languageBundleVersion
                    })
                )
            );
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
