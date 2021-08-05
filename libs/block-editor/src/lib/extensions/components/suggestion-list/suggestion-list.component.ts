import { FocusKeyManager } from '@angular/cdk/a11y';
import { AfterContentInit, Component, ContentChildren, HostListener, QueryList } from '@angular/core';
import { SuggestionsListItemComponent } from '../suggestions-list-item/suggestions-list-item.component';

@Component({
    selector: 'dotcms-suggestion-list',
    templateUrl: './suggestion-list.component.html',
    styleUrls: ['./suggestion-list.component.scss']
})
export class SuggestionListComponent implements AfterContentInit {
    private keyManager: FocusKeyManager<SuggestionsListItemComponent>;

    @ContentChildren(SuggestionsListItemComponent) items: QueryList<SuggestionsListItemComponent>;

    @HostListener('keydown', ['$event'])
    onKeydown(event: KeyboardEvent) {
        if (this.keyManager.activeItem) {
            this.keyManager.activeItem.unfocus();
        }

        this.keyManager.onKeydown(event);
    }

    // @Input() items = [];

    ngAfterContentInit() {
        this.keyManager = new FocusKeyManager(this.items).withWrap();
    }
}
