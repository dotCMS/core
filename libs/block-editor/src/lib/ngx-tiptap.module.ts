import { NgModule } from '@angular/core';

import { EditorDirective } from './editor.directive';
import { FloatingMenuDirective } from './floating-menu.directive';
import { BubbleMenuDirective } from './bubble-menu.directive';
import { DraggableDirective } from './draggable.directive';
import { NodeViewContentDirective } from './node-view-content.directive';
import { SuggestionListComponent } from './suggestion-list/suggestion-list.component';
import { SuggestionsComponent } from './suggestions/suggestions.component';

import { MenuModule } from 'primeng/menu';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ContentletBlockComponent } from './extentions/contentlet-block/contentlet-block.component';
import { SuggestionsService } from './services/suggestions.service';

@NgModule({
    imports: [CommonModule, MenuModule, CardModule],
    declarations: [
        EditorDirective,
        FloatingMenuDirective,
        BubbleMenuDirective,
        DraggableDirective,
        NodeViewContentDirective,
        SuggestionListComponent,
        SuggestionsComponent,
        ContentletBlockComponent
    ],
    providers: [SuggestionsService],
    exports: [
        SuggestionsComponent,
        EditorDirective,
        FloatingMenuDirective,
        BubbleMenuDirective,
        DraggableDirective,
        NodeViewContentDirective
    ]
})
export class NgxTiptapModule {}
