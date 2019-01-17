import { Directive, ElementRef, HostListener, Input } from '@angular/core';

/**
 * Directive that set a max length behavior to elements with contenteditable="true"
 * If not maxLength is specified the default is 255.
 */
@Directive({
    selector: '[dotMaxlength]'
})
export class DotMaxlengthDirective {
    private _maxLength: number;

    constructor(private el: ElementRef) {}

    @Input()
    set dotMaxlength(maxLength: number) {
        this._maxLength = maxLength || 255;
    }

    /**
     * Listener fo the paste & keypress event. Will reduce the string to max Length.
     *
     * @memberof DotMaxlengthDirective
     */
    @HostListener('paste', ['$event'])
    @HostListener('keypress', ['$event'])
    eventHandler() {
        setTimeout(() => {
            this.el.nativeElement.textContent = this.el.nativeElement.textContent.slice(
                0,
                this._maxLength
            );
        }, 0);
    }
}
