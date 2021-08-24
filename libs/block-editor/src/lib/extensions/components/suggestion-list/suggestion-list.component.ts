import { FocusKeyManager } from '@angular/cdk/a11y';
import { AfterContentInit, Component, ContentChildren, QueryList } from '@angular/core';
import { setTimeout } from 'timers';
import { SuggestionsListItemComponent } from '../suggestions-list-item/suggestions-list-item.component';

@Component({
    selector: 'dotcms-suggestion-list',
    templateUrl: './suggestion-list.component.html',
    styleUrls: ['./suggestion-list.component.scss']
})
export class SuggestionListComponent implements AfterContentInit {
    keyManager: FocusKeyManager<SuggestionsListItemComponent>;

    @ContentChildren(SuggestionsListItemComponent) items: QueryList<SuggestionsListItemComponent>;

    ngAfterContentInit() {
        this.keyManager = new FocusKeyManager(this.items).withWrap();
    }

    /**
     * Update the selected item in the list
     *
     * @param {KeyboardEvent} event
     * @memberof SuggestionListComponent
     */
    updateSelection(event: KeyboardEvent) {
        if (this.keyManager.activeItem) {
            this.keyManager.activeItem.unfocus();
        }

        this.keyManager.onKeydown(event);
    }

    /**
     * Execute the command function in the list item component
     *
     * @memberof SuggestionListComponent
     */
    execCommand() {
        this.keyManager.activeItem.command()
    }

    /**
     * Set the first item of the list active
     *
     * @memberof SuggestionListComponent
     */
    setFirstItemActive() {
        this.keyManager.activeItem?.unfocus();
        this.keyManager.setFirstItemActive();
        this.keyManager.activeItem.focus();
    }

    /**
     * Reset the key manager with the new options
     *
     * @memberof SuggestionListComponent
     */
    resetKeyManager() {
        this.keyManager = new FocusKeyManager(this.items).withWrap();

        // Needs to wait until the new items are rendered
        setTimeout(() => {
            this.setFirstItemActive();
        }, 0)
    }

    /**
     * Set an active item manually
     *
     * @param {number} index
     * @memberof SuggestionListComponent
     */
    updateActiveItem(index: number): void {
        this.keyManager.activeItem?.unfocus();
        this.keyManager.setActiveItem(index)
    }

}
