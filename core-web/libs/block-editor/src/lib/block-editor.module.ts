import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// DotCMS JS
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';

// Directives

// Nodes

// Extension Components

// Shared

//Editor
import { DotBlockEditorComponent } from './components/dot-block-editor/dot-block-editor.component';
import { DotEditorCountBarComponent } from './components/dot-editor-count-bar/dot-editor-count-bar.component';
import {
    ActionButtonComponent,
    BubbleLinkFormComponent,
    BubbleMenuButtonComponent,
    BubbleMenuComponent,
    DotImageService,
    DragHandlerComponent,
    FormActionsComponent,
    LoaderComponent,
    SuggestionPageComponent
} from './extensions';
import { BubbleFormComponent } from './extensions/bubble-form/bubble-form.component';
import { FloatingButtonComponent } from './extensions/floating-button/floating-button.component';
import { ImageTabviewFormModule } from './extensions/image-tabview-form/image-tabview-form.module';
import { ContentletBlockComponent } from './nodes';
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
        ImageTabviewFormModule
    ],
    declarations: [
        EditorDirective,
        ContentletBlockComponent,
        ActionButtonComponent,
        DragHandlerComponent,
        LoaderComponent,
        BubbleMenuComponent,
        BubbleMenuButtonComponent,
        BubbleLinkFormComponent,
        FormActionsComponent,
        BubbleFormComponent,
        SuggestionPageComponent,
        DotBlockEditorComponent,
        DotEditorCountBarComponent,
        FloatingButtonComponent
    ],
    providers: [DotImageService, LoggerService, StringUtils],
    exports: [
        EditorDirective,
        ActionButtonComponent,
        BubbleMenuComponent,
        BubbleLinkFormComponent,
        ReactiveFormsModule,
        SharedModule,
        BubbleFormComponent,
        DotBlockEditorComponent
    ]
})
export class BlockEditorModule {}
