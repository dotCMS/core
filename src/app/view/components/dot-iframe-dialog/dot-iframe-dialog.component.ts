import { Component, Input, SimpleChanges, OnChanges, EventEmitter, Output, HostListener, ViewChild } from '@angular/core';
import { Dialog } from 'primeng/primeng';
import { fromEvent } from 'rxjs/observable/fromEvent';

@Component({
    selector: 'dot-iframe-dialog',
    templateUrl: './dot-iframe-dialog.component.html',
    styleUrls: ['./dot-iframe-dialog.component.scss']
})
export class DotIframeDialogComponent implements OnChanges {
    @ViewChild('dialog') dialog: Dialog;

    @Input() url: string;
    @Input() header = '';

    @Output() beforeClose: EventEmitter<{
        originalEvent: MouseEvent | KeyboardEvent,
        close: () => void
    }> = new EventEmitter();
    @Output() close: EventEmitter<any> = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
    @Output() load: EventEmitter<any> = new EventEmitter();
    @Output() keydown: EventEmitter<KeyboardEvent> = new EventEmitter();

    show: boolean;

    constructor() {}

    ngOnChanges(changes: SimpleChanges) {
        if (changes.url) {
            this.show = !!changes.url.currentValue;

            // Need to wait til' the dialog is rendered
            setTimeout(() => {
                this.handleMaskEvents(this.show);
            }, 0);
        }

        if (changes.header) {
            this.header = changes.header.currentValue;
        }
    }

    /**
     * Handle attemp to close the dialog
     *
     * @param {(MouseEvent | KeyboardEvent)} $event
     * @memberof DotIframeDialogComponent
     */
    onClose($event: MouseEvent | KeyboardEvent): void {
        if ($event.preventDefault) {
            $event.preventDefault();
        }

        if (this.beforeClose.observers.length) {
            this.beforeClose.emit({
                originalEvent: $event,
                close: () => {
                    this.closeDialog();
                }
            });
        } else {
            this.closeDialog();
        }
    }

    /**
     * Handle custom event from the iframe window
     *
     * @param {CustomEvent} $event
     * @memberof DotIframeDialogComponent
     */
    onCustomEvents($event: CustomEvent): void {
        this.custom.emit($event);
    }

    /**
     * Handle keydown event from the iframe window
     *
     * @param {any} $event
     * @memberof DotIframeDialogComponent
     */
    @HostListener('document:keydown', ['$event'])
    onKeyDown($event: KeyboardEvent): void {
        this.keydown.emit($event);

        if ($event.key === 'Escape') {
            this.onClose($event);
        }
    }

    /**
     * Handle load event from the iframe window
     *
     * @param {*} $event
     * @memberof DotIframeDialogComponent
     */
    onLoad($event: any): void {
        $event.target.contentWindow.focus();
        this.load.emit($event);
    }

    private closeDialog(): void {
        this.url = null;
        this.show = false;
        this.header = '';
        this.close.emit();
    }

    private handleMaskEvents(show: boolean): void {
        if (show) {
            fromEvent(this.dialog.mask, 'click').subscribe(this.onClose.bind(this));
        }
    }
}
