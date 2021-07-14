import { NgModule } from '@angular/core';

import { EditorDirective } from './editor.directive';
import { BubbleMenuDirective } from './bubble-menu.directive';
import { DraggableDirective } from './draggable.directive';
import { NodeViewContentDirective } from './node-view-content.directive';
import { SuggestionsComponent } from './suggestions/suggestions.component';

import { MenuModule } from 'primeng/menu';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ContentletBlockComponent } from './extentions/blocks/contentlet-block/contentlet-block.component';

import { SuggestionsService } from './services/suggestions.service';
import { SuggestionListComponent } from './suggestion-list/suggestion-list.component';
import { ActionButtonComponent } from './extentions/action-button/action-button.component';

@NgModule({
    imports: [CommonModule, CardModule, MenuModule],
    declarations: [
        EditorDirective,
        BubbleMenuDirective,
        DraggableDirective,
        NodeViewContentDirective,
        SuggestionsComponent,
        SuggestionListComponent,
        ContentletBlockComponent,
        ActionButtonComponent
    ],
    providers: [SuggestionsService],
    exports: [
        SuggestionsComponent,
        EditorDirective,
        BubbleMenuDirective,
        DraggableDirective,
        NodeViewContentDirective,
        ActionButtonComponent
    ]
})
export class NgxTiptapModule { }
