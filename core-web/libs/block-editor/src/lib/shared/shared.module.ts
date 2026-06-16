import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { DotContentletStatusBadgeComponent } from '@dotcms/ui';

// Shared
import {
    ContentletStatePipe,
    EmptyMessageComponent,
    SuggestionListComponent,
    SuggestionLoadingListComponent,
    SuggestionsComponent,
    SuggestionsListItemComponent
} from './';

import { SuggestionsService } from './services';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        TagModule,
        DotContentletStatusBadgeComponent
    ],
    declarations: [
        SuggestionsComponent,
        SuggestionListComponent,
        SuggestionsListItemComponent,
        SuggestionLoadingListComponent,
        ContentletStatePipe,
        EmptyMessageComponent
    ],
    providers: [SuggestionsService],
    exports: [
        SuggestionsComponent,
        SuggestionListComponent,
        SuggestionsListItemComponent,
        SuggestionLoadingListComponent,
        EmptyMessageComponent,
        ContentletStatePipe
    ]
})
export class SharedModule {}
