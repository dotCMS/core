import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Shared
import { SuggestionsService } from './services';
import {
    SuggestionsComponent,
    SuggestionListComponent,
    SuggestionsListItemComponent,
    SuggestionLoadingListComponent,
    ContentletStatePipe
} from './';

@NgModule({
    imports: [CommonModule, FormsModule, ReactiveFormsModule],
    declarations: [
        SuggestionsComponent,
        SuggestionListComponent,
        SuggestionsListItemComponent,
        SuggestionLoadingListComponent,
        ContentletStatePipe
    ],
    providers: [SuggestionsService],
    exports: [SuggestionsComponent]
})
export class SharedModule {}
