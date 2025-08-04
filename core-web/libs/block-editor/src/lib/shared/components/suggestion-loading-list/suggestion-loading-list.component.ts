import { Component } from '@angular/core';

@Component({
    selector: 'dot-suggestion-loading-list',
    templateUrl: './suggestion-loading-list.component.html',
    styleUrls: ['./suggestion-loading-list.component.scss'],
    standalone: false
})
export class SuggestionLoadingListComponent {
    items = Array(4).fill(0);
}
