import { Component, Input, ViewChild } from '@angular/core';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';
import { DotMenuItem } from '../suggestions/suggestions.component';

@Component({
    selector: 'dot-suggestion-page',
    templateUrl: './suggestion-page.component.html',
    styleUrls: ['./suggestion-page.component.scss']
})
export class SuggestionPageComponent {
    // TODO: Move all the logic related to the list to its component
    @ViewChild('list', { static: false }) list: SuggestionListComponent;

    // Maybe this should be an @Output() instead of @Input();
    @Input() items: DotMenuItem[] = [];
    @Input() loading = false;

    constructor() {
        /** */
    }
}
