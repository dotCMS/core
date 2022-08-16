import { NgModule } from '@angular/core';

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
