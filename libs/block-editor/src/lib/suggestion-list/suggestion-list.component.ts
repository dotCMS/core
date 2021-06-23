import { Component, Input } from '@angular/core';

@Component({
    selector: 'dotcms-suggestion-list',
    templateUrl: './suggestion-list.component.html',
    styleUrls: ['./suggestion-list.component.scss']
})
export class SuggestionListComponent {
    @Input() items = [];
}
