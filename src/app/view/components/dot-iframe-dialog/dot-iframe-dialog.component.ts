import {
    Component,
    Input,
    SimpleChanges,
    OnChanges,
    EventEmitter,
    Output,
    ViewChild,
    OnInit
} from '@angular/core';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { filter } from 'rxjs/operators';

@Component({
    selector: 'dot-iframe-dialog',
    templateUrl: './dot-iframe-dialog.component.html',
    styleUrls: ['./dot-iframe-dialog.component.scss']
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
    close: EventEmitter<any> = new EventEmitter();

    @Output()
    custom: EventEmitter<CustomEvent> = new EventEmitter();

    @Output()
    load: EventEmitter<any> = new EventEmitter();

    @Output()
    keydown: EventEmitter<KeyboardEvent> = new EventEmitter();

    show: boolean;

    constructor() {}

    ngOnInit() {
        if (this.beforeClose.observers.length) {
            this.dotDialog.beforeClose
                .pipe(filter(() => this.show))
                .subscribe((event: {
                    close: () => void;
                }) => {
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
        this.keydown.emit($event);

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
    onLoad($event: any): void {
        $event.target.contentWindow.focus();
        this.load.emit($event);
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
        this.close.emit();
    }
}
