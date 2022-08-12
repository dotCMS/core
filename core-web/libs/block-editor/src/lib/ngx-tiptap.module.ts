import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// PrimeNg
import { MenuModule } from 'primeng/menu';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';

// DotCMS JS
import { LoggerService } from '@dotcms/dotcms-js';
import { StringUtils } from '@dotcms/dotcms-js';

// Directives
import {
    EditorDirective,
    BubbleMenuDirective,
    DraggableDirective,
    NodeViewContentDirective
} from './directives';

// Services
import { SuggestionsService, DotImageService } from './services';

// Nodes
import { ContentletBlockComponent, ImageBlockComponent } from './nodes';

// Extension Components
import {
    ActionButtonComponent,
    BubbleLinkFormComponent,
    BubbleMenuButtonComponent,
    BubbleMenuComponent,
    DragHandlerComponent,
    FormActionsComponent,
    LoaderComponent
} from './extensions';

// Shared
import {
    SuggestionsComponent,
    SuggestionListComponent,
    SuggestionsListItemComponent,
    SuggestionLoadingListComponent,
    ContentletStatePipe
} from './shared';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        CardModule,
        MenuModule,
        CheckboxModule,
        ButtonModule,
        InputTextModule
    ],
    declarations: [
        EditorDirective,
        BubbleMenuDirective,
        DraggableDirective,
        NodeViewContentDirective,
        SuggestionsComponent,
        SuggestionListComponent,
        SuggestionsListItemComponent,
        ContentletBlockComponent,
        ActionButtonComponent,
        DragHandlerComponent,
        ImageBlockComponent,
        LoaderComponent,
        BubbleMenuComponent,
        BubbleMenuButtonComponent,
        BubbleLinkFormComponent,
        ContentletStatePipe,
        SuggestionLoadingListComponent,
        FormActionsComponent
    ],
    providers: [SuggestionsService, DotImageService, LoggerService, StringUtils],
    exports: [
        SuggestionsComponent,
        EditorDirective,
        BubbleMenuDirective,
        DraggableDirective,
        NodeViewContentDirective,
        ActionButtonComponent,
        BubbleMenuComponent,
        BubbleLinkFormComponent,
        ReactiveFormsModule,
        CheckboxModule,
        ButtonModule,
        InputTextModule
    ]
})
export class NgxTiptapModule {}
