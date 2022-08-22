import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { SuggestionListComponent, DotMenuItem } from '@dotcms/block-editor';

@Component({
    selector: 'dot-suggestion-page',
    templateUrl: './suggestion-page.component.html',
    styleUrls: ['./suggestion-page.component.scss']
})
export class SuggestionPageComponent {
    @ViewChild('list', { static: false }) list: SuggestionListComponent;

    @Input() items: DotMenuItem[] = [];
    @Input() loading = false;
    @Input() title: string;

    @Output() back = new EventEmitter<boolean>();

    /**
     * Go back to contentlet selection
     *
     * @memberof SuggestionPageComponent
     */
    handleBackButton(): boolean {
        this.back.emit(true);

        return false;
    }

    /**
     * Execute the item command
     *
     * @memberof SuggestionPageComponent
     */
    execCommand() {
        this.items.length ? this.list.execCommand() : this.handleBackButton();
    }

    /**
     * Update the current item selected
     *
     * @param {KeyboardEvent} e
     * @memberof SuggestionPageComponent
     */
    updateSelection(e: KeyboardEvent) {
        this.list.updateSelection(e);
    }
}
