import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

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
    imports: [CommonModule, FormsModule, ReactiveFormsModule, ButtonModule],
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
