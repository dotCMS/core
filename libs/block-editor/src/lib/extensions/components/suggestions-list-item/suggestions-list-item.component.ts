import { Component, ElementRef, HostBinding } from '@angular/core';

import { FocusableOption } from '@angular/cdk/a11y';

@Component({
    selector: 'dotcms-suggestions-list-item',
    templateUrl: './suggestions-list-item.component.html',
    styleUrls: ['./suggestions-list-item.component.scss'],
})
export class SuggestionsListItemComponent implements FocusableOption {
    @HostBinding('attr.role') role = 'list-item'
    @HostBinding('attr.tabindex') tabindex = '-1'

    constructor(private element: ElementRef) { }

    getLabel(): string {
        return this.element.nativeElement.innerText;
    }

    focus() {
        this.element.nativeElement.style = 'background: lightgray';
    }

    unfocus() {
        this.element.nativeElement.style = '';
    }
}
