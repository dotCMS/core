import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';

import { filter } from 'rxjs/operators';

import { DotDialogComponent } from '@dotcms/ui';

import { IframeComponent } from '../_common/iframe/iframe-component/iframe.component';

@Component({
    selector: 'dot-iframe-dialog',
    templateUrl: './dot-iframe-dialog.component.html',
    styleUrls: ['./dot-iframe-dialog.component.scss'],
    imports: [DotDialogComponent, IframeComponent]
})
export class DotIframeDialogComponent implements OnChanges, OnInit {
    @ViewChild('dialog', { static: true })
    dotDialog: DotDialogComponent;

    @Input()
    url: string;

    @Input()
    header = '';

    @Output()
    beforeClose: EventEmitter<{
        close: () => void;
    }> = new EventEmitter();

    @Output()
    shutdown: EventEmitter<void> = new EventEmitter();

    @Output()
    custom: EventEmitter<CustomEvent<Record<string, unknown>>> = new EventEmitter();

    @Output()
    charge: EventEmitter<unknown> = new EventEmitter();

    @Output()
    keyWasDown: EventEmitter<KeyboardEvent> = new EventEmitter();

    show: boolean;

    ngOnInit() {
        if (this.beforeClose.observers.length) {
            this.dotDialog.beforeClose
                .pipe(filter(() => this.show))
                .subscribe((event: { close: () => void }) => {
                    this.beforeClose.emit(event);
                });
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.url) {
            this.show = !!changes.url.currentValue;
        }

        if (changes.header) {
            this.header = changes.header.currentValue;
        }
    }

    /**
     * Handle keydown event from the iframe window
     *
     * @param any $event
     * @memberof DotIframeDialogComponent
     */
    onKeyDown($event: KeyboardEvent): void {
        this.keyWasDown.emit($event);

        if ($event.key === 'Escape') {
            this.dotDialog.close();
        }
    }

    /**
     * Handle load event from the iframe window
     *
     * @param * $event
     * @memberof DotIframeDialogComponent
     */
    onLoad($event: { target: HTMLIFrameElement }): void {
        $event.target.contentWindow.focus();
        this.charge.emit($event);
    }

    /**
     * Handle dialog close/hide
     *
     * @memberof DotIframeDialogComponent
     */
    onDialogHide(): void {
        this.url = null;
        this.show = false;
        this.header = '';
        this.shutdown.emit();
    }
}
