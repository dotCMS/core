import { Component, Input, SimpleChanges, OnChanges, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'dot-iframe-dialog',
    templateUrl: './dot-iframe-dialog.component.html',
    styleUrls: ['./dot-iframe-dialog.component.scss']
})
export class DotIframeDialogComponent implements OnChanges {
    @Input() url: string;
    @Output() close: EventEmitter<any> = new EventEmitter();
    @Output() load: EventEmitter<any> = new EventEmitter();
    @Output() keydown: EventEmitter<KeyboardEvent> = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();

    show: boolean;

    constructor() {}

    ngOnChanges(changes: SimpleChanges) {
        this.show = !!changes.url.currentValue;
    }

    /**
     * Callback when dialog hide
     *
     * @memberof DotIframeDialogComponent
     */
    closeDialog(): void {
        this.url = null;
        this.show = false;
        this.close.emit();
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
    onKeyDown($event: KeyboardEvent): void {
        this.keydown.emit($event);

        if ($event.key === 'Escape') {
            this.closeDialog();
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
}
