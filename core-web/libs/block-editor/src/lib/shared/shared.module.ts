import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';

// Shared
import { SuggestionsService } from './services';
import {
    SuggestionsComponent,
    SuggestionListComponent,
    SuggestionsListItemComponent,
    SuggestionLoadingListComponent,
    EmptyMessageComponent,
    ContentletStatePipe
} from './';

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
        EmptyMessageComponent
    ]
})
export class SharedModule {}
