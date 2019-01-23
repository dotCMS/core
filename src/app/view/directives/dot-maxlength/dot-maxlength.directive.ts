import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { fromEvent, merge, Subject } from 'rxjs';
import { delay, filter, takeUntil, tap } from 'rxjs/operators';

/**
 * Directive that set a max length behavior to elements with contenteditable="true"
 * If not maxLength is specified the default is 255.
 */
@Directive({
    selector: '[dotMaxlength]'
})
export class DotMaxlengthDirective implements OnInit, OnDestroy {
    private _maxLength: number;
    private events = ['paste', 'keypress'];
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private el: ElementRef) {}

    ngOnInit() {
        const eventStreams = this.events.map(ev => fromEvent(this.el.nativeElement, ev));
        const allEvents$ = merge(...eventStreams);
        allEvents$
            .pipe(
                takeUntil(this.destroy$),
                tap((keyboardEvent: Event) => {
                    if (!this.isValidAction(keyboardEvent)) {
                        keyboardEvent.preventDefault();
                    }
                }),
                delay(1),
                filter(() => this.el.nativeElement.textContent.length > this._maxLength),
                tap(() => this.reduceText())
            )
            .subscribe();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    @Input()
    set dotMaxlength(maxLength: number) {
        this._maxLength = maxLength || 255;
    }

    private isValidAction(keyboardEvent: Event): boolean {
        return (
            this.el.nativeElement.textContent.length < this._maxLength ||
            this.isAllowedKeyCode(keyboardEvent) ||
            !!window.getSelection().toString()
        );
    }
    private reduceText(): void {
        this.el.nativeElement.textContent = this.el.nativeElement.textContent.slice(
            0,
            this._maxLength
        );
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
