import { Subject } from 'rxjs';

import { FocusKeyManager } from '@angular/cdk/a11y';
import {
    AfterViewInit,
    Component,
    ContentChildren,
    HostBinding,
    HostListener,
    Input,
    OnDestroy,
    QueryList
} from '@angular/core';

// Components
import { takeUntil } from 'rxjs/operators';

import { SuggestionsListItemComponent } from './components/suggestions-list-item/suggestions-list-item.component';

// Interfaces

import { DotMenuItem } from '../suggestions/suggestions.component';

@Component({
    selector: 'dot-suggestion-list',
    templateUrl: './suggestion-list.component.html',
    styleUrls: ['./suggestion-list.component.scss'],
    standalone: false
})
export class SuggestionListComponent implements AfterViewInit, OnDestroy {
    @ContentChildren(SuggestionsListItemComponent) items: QueryList<SuggestionsListItemComponent>;
    @HostBinding('attr.id') id = 'editor-suggestion-list';
    @Input() suggestionItems: DotMenuItem[] = [];

    keyManager: FocusKeyManager<SuggestionsListItemComponent>;
    private destroy$ = new Subject<boolean>();
    private mouseMove = true;

    @HostListener('mousemove', ['$event'])
    onMouseMove() {
        this.mouseMove = true;
    }

    /**
     * Handle the active item on menu events
     *
     * @param {MouseEvent} e
     * @memberof SuggestionListComponent
     */
    @HostListener('mouseover', ['$event'])
    onMouseOver(e: MouseEvent) {
        const element = e.target as HTMLElement;
        const value = element.dataset?.index as unknown;

        if (isNaN(value as number) || !this.mouseMove) {
            return;
        }

        const index = Number(element?.dataset.index);

        if (element.getAttribute('disabled')) {
            this.keyManager.activeItem?.unfocus();
        } else {
            this.updateActiveItem(index);
        }
    }

    /**
     * Avoid closing the suggestions on manual scroll
     *
     * @param {MouseEvent} e
     * @memberof SuggestionListComponent
     */
    @HostListener('mousedown', ['$event'])
    onMouseDownHandler(e: MouseEvent) {
        e.preventDefault();
    }

    ngAfterViewInit() {
        this.keyManager = new FocusKeyManager(this.items).withWrap();
        requestAnimationFrame(() => this.setFirstItemActive());

        this.items.changes
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => requestAnimationFrame(() => this.setFirstItemActive()));
    }

    ngOnDestroy() {
        this.destroy$.next(true);
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
}
