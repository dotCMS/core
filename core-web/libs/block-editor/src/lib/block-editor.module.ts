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
import { SkeletonModule } from 'primeng/skeleton';
import { ScrollerModule } from 'primeng/scroller';

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
import { ImageTabviewFormComponent } from './extensions/image-tabview-form/image-tabview-form.component';
import { DotImageCardListComponent } from './extensions/image-tabview-form/components/dot-image-card-list/dot-image-card-list.component';
import { DotImageCardComponent } from './extensions/image-tabview-form/components/dot-image-card/dot-image-card.component';
import { DotImageCardSkeletonComponent } from './extensions/image-tabview-form/components/dot-image-card-skeleton/dot-image-card-skeleton.component';

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
        SkeletonModule,
        ScrollerModule
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
        ImageTabviewFormComponent,
        DotImageCardListComponent,
        DotImageCardComponent,
        DotImageCardSkeletonComponent
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
