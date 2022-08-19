import {
    Component,
    HostListener,
    Input,
    QueryList,
    AfterViewInit,
    ViewChildren,
    OnChanges
} from '@angular/core';
import { FocusKeyManager } from '@angular/cdk/a11y';

// Prime NG
import { MenuItem } from 'primeng/api';

// Components
import { SuggestionsListItemComponent } from './components/suggestions-list-item/suggestions-list-item.component';

// Interfaces
import { DotMenuItem } from '../suggestions/suggestions.component';
import { SimpleChanges } from '@angular/core';

@Component({
    selector: 'dot-suggestion-list',
    templateUrl: './suggestion-list.component.html',
    styleUrls: ['./suggestion-list.component.scss']
})
export class SuggestionListComponent implements AfterViewInit, OnChanges {
    // This should not be a suggestion list component
    // It must be an interface with two outputs (mousedown) and (mouseenter)
    @ViewChildren(SuggestionsListItemComponent) items: QueryList<SuggestionsListItemComponent>;
    @Input() suggestionItems: DotMenuItem[] = [];
    @Input() urlItem = false;

    keyManager: FocusKeyManager<SuggestionsListItemComponent>;
    private mouseMove = true;

    @HostListener('mousemove', ['$event'])
    onMousemove() {
        this.mouseMove = true;
    }

    // @HostListener('window:keydown', ['$event'])
    // keyEvent(event: KeyboardEvent) {

    //     const { key } = event;

    //     console.log("Entramos");

    //     // if (key === 'Enter') {
    //     //     this.execCommand();

    //     //     return false;
    //     // }

    //     // // I think this must be handled by the suggestion list component.
    //     if (key === 'ArrowDown' || key === 'ArrowUp') {
    //         this.updateSelection(event);

    //         return false;
    //     }
    //     // return false;
    // }

    ngAfterViewInit() {
        this.keyManager = new FocusKeyManager(this.items).withWrap();
        requestAnimationFrame(() => this.setFirstItemActive());
    }

    ngOnChanges(changes: SimpleChanges) {
        const { suggestionItems } = changes;

        if (suggestionItems.firstChange) {
            return;
        }

        requestAnimationFrame(() => this.resetKeyManager());
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
        this.keyManager.activeItem?.scrollIntoView();
        this.mouseMove = false;
    }

    /**
     * Execute the command function in the list item component
     *
     * @memberof SuggestionListComponent
     */
    execCommand() {
        this.keyManager.activeItem.command();
    }

    /**
     * Set the first item of the list active
     *
     * @memberof SuggestionListComponent
     */
    setFirstItemActive() {
        this.keyManager.activeItem?.unfocus();
        this.keyManager.setFirstItemActive();
        this.keyManager.activeItem?.focus();
    }

    /**
     * Reset the key manager with the new options
     *
     * @memberof SuggestionListComponent
     */
    resetKeyManager() {
        this.keyManager.activeItem?.unfocus();
        this.keyManager = new FocusKeyManager(this.items).withWrap();
        this.setFirstItemActive();
    }

    /**
     * Set an active item manually
     *
     * @param {number} index
     * @memberof SuggestionListComponent
     */
    updateActiveItem(index: number): void {
        this.keyManager.activeItem?.unfocus();
        this.keyManager.setActiveItem(index);
    }

    /**
     * Execute the item command on mouse down
     *
     * @param {MouseEvent} e
     * @param {MenuItem} item
     * @memberof SuggestionsComponent
     */
    onMouseDown(e: MouseEvent, item: MenuItem) {
        e.preventDefault();
        item.command();
    }

    /**
     * Avoid closing the suggestions on manual scroll
     *
     * @param {MouseEvent} e
     * @memberof SuggestionsComponent
     */
    onMouseDownHandler(e: MouseEvent) {
        e.preventDefault();
    }

    /**
     * Handle the active item on menu events
     *
     * @param {MouseEvent} e
     * @memberof SuggestionsComponent
     */
    onMouseEnter(e: MouseEvent) {
        // If mouse does not move then leave the function.
        if (!this.mouseMove) {
            return;
        }

        e.preventDefault();
        const index = Number((e.target as HTMLElement).dataset.index);
        this.updateActiveItem(index);
    }
}
