import { FocusableOption } from '@angular/cdk/a11y';
import { Component, ElementRef, HostBinding, HostListener, Input, OnInit } from '@angular/core';

@Component({
    selector: 'dot-suggestions-list-item',
    templateUrl: './suggestions-list-item.component.html',
    styleUrls: ['./suggestions-list-item.component.scss']
})
export class SuggestionsListItemComponent implements FocusableOption, OnInit {
    @HostBinding('attr.role') role = 'list-item';
    @HostBinding('attr.tabindex') tabindex = '-1';
    @HostBinding('attr.disabled') @Input() disabled = false;

    @HostBinding('attr.data-index')
    @Input()
    index: string;

    @Input() command: () => void;
    @Input() label = '';
    @Input() url = '';
    @Input() page = false;
    @Input() data = null;

    icon = false;

    constructor(private element: ElementRef) {}

    @HostListener('mousedown', ['$event'])
    onMouseDown(e: MouseEvent) {
        e.preventDefault();
        if (!this.disabled) {
            this.command();
        }
    }

    ngOnInit() {
        this.icon = typeof this.url === 'string' && !(this.url.split('/').length > 1);
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

    scrollIntoView() {
        if (!this.isIntoView()) {
            const child = this.element.nativeElement as HTMLElement;
            const parent = child.parentElement;

            // Get BoundingClientRect of the elements
            const {
                top: containerTop,
                top: containerBot,
                height: containerHeight
            } = parent.getBoundingClientRect();
            const { top, bottom } = child.getBoundingClientRect();

            const scrollTop = top - containerTop;
            const scrollBot = bottom - containerBot;

            // += scrollTop -> If we're near the top of the list.
            // += scrollBot - containerHeight -> If we're near the bottom of the list.
            parent.scrollTop += this.alignToTop() ? scrollTop : scrollBot - containerHeight;
        }
    }

    /**
     *
     * Check if the element is a visible area
     *
     * @private
     * @return {*}  {boolean}
     * @memberof SuggestionsListItemComponent
     */
    private isIntoView(): boolean {
        const { bottom, top } = this.element.nativeElement.getBoundingClientRect();
        const containerRect = this.element.nativeElement.parentElement.getBoundingClientRect();

        return top >= containerRect.top && bottom <= containerRect.bottom;
    }

    /**
     *
     * If true, the top of the element will be aligned to the top of the visible area
     * of the scrollable ancestor If true, the top of the element will be aligned to
     * the top of the visible area of the scrollable ancestor.
     *
     * @private
     * @return {*}  {boolean}
     * @memberof SuggestionsListItemComponent
     */
    private alignToTop(): boolean {
        const { top } = this.element.nativeElement.getBoundingClientRect();
        const { top: containerTop } =
            this.element.nativeElement.parentElement.getBoundingClientRect();

        return top < containerTop;
    }
}
