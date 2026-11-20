import { TiptapBubbleMenuDirective } from 'ngx-tiptap';

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

import {
    DotContentSearchService,
    DotLanguagesService,
    DotLocalstorageService,
    initializeLegacyTranslations,
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
        provideAppInitializer(() =>
            initializeLegacyTranslations(
                inject(DotMessageService),
                inject(DotcmsConfigService),
                inject(DotLocalstorageService),
                inject(LoggerService)
            )
        )
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
