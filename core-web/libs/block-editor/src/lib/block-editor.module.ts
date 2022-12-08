import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// PrimeNg
import { MenuModule } from 'primeng/menu';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';
import { OrderListModule } from 'primeng/orderlist';
import { ListboxModule } from 'primeng/listbox';
import { TabViewModule } from 'primeng/tabview';
import { VirtualScrollerModule } from 'primeng/virtualscroller';
import { TableModule } from 'primeng/table';
import { SkeletonModule } from 'primeng/skeleton';

// DotCMS JS
import { LoggerService } from '@dotcms/dotcms-js';
import { StringUtils } from '@dotcms/dotcms-js';

// Directives
import { EditorDirective } from './shared/directives';

// Nodes
import { ContentletBlockComponent } from './nodes';

// Extension Components
import {
    ActionButtonComponent,
    BubbleLinkFormComponent,
    BubbleMenuButtonComponent,
    BubbleMenuComponent,
    DragHandlerComponent,
    FormActionsComponent,
    LoaderComponent,
    DotImageService,
    SuggestionPageComponent
} from './extensions';

// Shared
import { SharedModule } from './shared/shared.module';
import { BubbleFormComponent } from './extensions/bubble-form/bubble-form.component';

//Editor
import { DotBlockEditorComponent } from './components/dot-block-editor/dot-block-editor.component';
import { DotEditorCountBarComponent } from './components/dot-editor-count-bar/dot-editor-count-bar.component';
import { FloatingButtonComponent } from './extensions/floating-button/floating-button.component';
import { ImageFormComponent } from './extensions/image-form/image-form.component';
import { SearchTabComponent } from './extensions/image-form/components/search-tab/search-tab.component';
import { ImageCardComponent } from './extensions/image-form/components/image-card/image-card.component';
import { ImageCardSkeletonComponent } from './extensions/image-form/components/image-card-skeleton/image-card-skeleton.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        CardModule,
        MenuModule,
        CheckboxModule,
        ButtonModule,
        InputTextModule,
        SharedModule,
        OrderListModule,
        ListboxModule,
        TabViewModule,
        VirtualScrollerModule,
        TableModule,
        SkeletonModule
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
        FloatingButtonComponent,
        ImageFormComponent,
        SearchTabComponent,
        ImageCardComponent,
        ImageCardSkeletonComponent
    ],
    providers: [DotImageService, LoggerService, StringUtils],
    exports: [
        EditorDirective,
        ActionButtonComponent,
        BubbleMenuComponent,
        BubbleLinkFormComponent,
        ReactiveFormsModule,
        CheckboxModule,
        ButtonModule,
        InputTextModule,
        SharedModule,
        BubbleFormComponent,
        DotBlockEditorComponent
    ]
})
export class BlockEditorModule {}
