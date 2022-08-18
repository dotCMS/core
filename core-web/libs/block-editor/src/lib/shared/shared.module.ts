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
import { SuggestionPageComponent } from './components/suggestion-page/suggestion-page.component';

@NgModule({
    imports: [CommonModule, FormsModule, ReactiveFormsModule],
    declarations: [
        SuggestionsComponent,
        SuggestionListComponent,
        SuggestionsListItemComponent,
        SuggestionLoadingListComponent,
        ContentletStatePipe,
        SuggestionPageComponent
    ],
    providers: [SuggestionsService],
    exports: [SuggestionsComponent, SuggestionPageComponent]
})
export class SharedModule {}
