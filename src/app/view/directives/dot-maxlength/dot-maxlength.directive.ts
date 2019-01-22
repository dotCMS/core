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
    eventHandler(event: Event) {
        if (
            this.el.nativeElement.textContent.length < this._maxLength ||
            this.isAllowedKeyCode(event) ||
            !!window.getSelection().toString()
        ) {
            setTimeout(() => {
                if (this.el.nativeElement.textContent.length > this._maxLength) {
                    this.el.nativeElement.textContent = this.el.nativeElement.textContent.slice(
                        0,
                        this._maxLength
                    );
                }
            }, 0);
        } else {
            event.preventDefault();
        }
    }

    /**
     * Check if a keycode is allowed when max limit is reached
     * 8 : Backspace
     * 37: LeftKey
     * 38: UpKey
     * 39: RightKey
     * 40: DownKey
     * ctrlKey for control key
     * metakey for command key on mac keyboard
     * @param {any} eventKeycode
     * @returns boolean
     *
     * @memberof DotMaxlengthDirective
     */
    isAllowedKeyCode(event) {
        return (
            event.keyCode === 8 ||
            event.keyCode === 38 ||
            event.keyCode === 39 ||
            event.keyCode === 37 ||
            event.keyCode === 40 ||
            event.ctrlKey ||
            event.metaKey
        );
    }
}
