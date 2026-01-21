import { fromEvent, merge, Subject } from 'rxjs';

import { Directive, ElementRef, Input, OnDestroy, OnInit, inject } from '@angular/core';

import { delay, filter, takeUntil, tap } from 'rxjs/operators';

/**
 * Directive that set a max length behavior to elements with contenteditable="true"
 * If not maxLength is specified the default is 255.
 */
@Directive({
    selector: '[dotMaxlength]'
})
export class DotMaxlengthDirective implements OnInit, OnDestroy {
    private el = inject(ElementRef);

    private _maxLength: number;
    private events = ['paste', 'keypress'];
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private allowedEvents = ['Backspace', 'ArrowLeft', 'ArrowUp', 'ArrowRight', 'ArrowDown'];

    ngOnInit() {
        const eventStreams = this.events.map((ev) => fromEvent(this.el.nativeElement, ev));
        const allEvents$ = merge(...eventStreams);
        allEvents$
            .pipe(
                takeUntil(this.destroy$),
                tap((keyboardEvent: KeyboardEvent) => {
                    if (!this.isValidAction(keyboardEvent)) {
                        keyboardEvent.preventDefault();
                    }
                }),
                delay(1),
                filter(() => this.el.nativeElement.textContent.length > this._maxLength)
            )
            .subscribe(() => {
                this.reduceText();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    @Input()
    set dotMaxlength(maxLength: number) {
        this._maxLength = maxLength || 255;
    }

    private isValidAction(event: KeyboardEvent): boolean {
        return (
            this.el.nativeElement.textContent.length < this._maxLength ||
            this.isAllowedKeyCode(event) ||
            !!window.getSelection().toString()
        );
    }
    private reduceText(): void {
        this.el.nativeElement.textContent = this.el.nativeElement.textContent.slice(
            0,
            this._maxLength
        );
    }

    private isAllowedKeyCode(event: KeyboardEvent) {
        return this.allowedEvents.includes(event.key) || event.ctrlKey || event.metaKey;
    }
}
