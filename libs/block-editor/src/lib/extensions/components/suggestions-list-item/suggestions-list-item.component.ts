import { Component, ElementRef, HostBinding, Input, OnInit } from '@angular/core';

import { FocusableOption } from '@angular/cdk/a11y';

@Component({
    selector: 'dotcms-suggestions-list-item',
    templateUrl: './suggestions-list-item.component.html',
    styleUrls: ['./suggestions-list-item.component.scss'],
})
export class SuggestionsListItemComponent implements FocusableOption, OnInit {
    @HostBinding('attr.role') role = 'list-item'
    @HostBinding('attr.tabindex') tabindex = '-1'

    @HostBinding('attr.data-index')
    @Input() index: number;

    @Input() command: () => void;
    @Input() label = '';
    @Input() url = '';

    icon = false;

    constructor(private element: ElementRef) { }

    ngOnInit() {
        this.icon = this.icon = typeof( this.url ) === 'string' && !(this.url.split('/').length > 1);
    }

    getLabel(): string {
        return this.element.nativeElement.innerText;
    }

    focus() {
        this.element.nativeElement.style = 'background: #eee';
    }

    unfocus() {
        this.element.nativeElement.style = '';
    }
}
