import { DOCUMENT } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
    ChangeDetectionStrategy,
    inject
} from '@angular/core';

import { DialogModule, Dialog } from 'primeng/dialog';

import { hasOverlayAbove } from '@dotcms/ui';

import { IframeComponent } from '../_common/iframe/iframe-component/iframe.component';

@Component({
    selector: 'dot-iframe-dialog',
    templateUrl: './dot-iframe-dialog.component.html',
    styleUrls: ['./dot-iframe-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DialogModule, IframeComponent]
})
export class DotIframeDialogComponent implements OnChanges {
    readonly #document = inject(DOCUMENT);

    @ViewChild('dialog', { static: true })
    dotDialog: Dialog;

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

    ngOnChanges(changes: SimpleChanges) {
        if (changes.url) {
            this.show = !!changes.url.currentValue;
        }

        if (changes.header) {
            this.header = changes.header.currentValue;
        }
    }

    /**
     * A keydown inside the dialog's iframe. Escape closes the dialog, unless a layer sits above it
     * (a side panel opened from inside it, #38303): then the Escape is that layer's, so it is handed
     * to the page, where the panel's Escape handling hears it, and the dialog stays open.
     *
     * @param $event The keydown from the iframe's window.
     */
    onKeyDown($event: KeyboardEvent): void {
        this.keyWasDown.emit($event);

        if ($event.key !== 'Escape') {
            return;
        }

        if (this.#layerAbove()) {
            this.#document.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true })
            );

            return;
        }

        this.onDialogHide();
    }

    /**
     * The iframe loaded: it takes the keyboard, unless a layer sits above the dialog. The Workflow
     * Tasks detail reloads behind a side panel after each save; focusing it then would send the
     * author's next Escape into the iframe instead of to the panel (#38303).
     *
     * @param $event The iframe's load.
     */
    onLoad($event: { target: HTMLIFrameElement }): void {
        if (!this.#layerAbove()) {
            $event.target.contentWindow.focus();
        }

        this.charge.emit($event);
    }

    /** Whether an overlay (a side panel, another dialog) is stacked above this dialog. */
    #layerAbove(): boolean {
        return hasOverlayAbove(this.dotDialog?.container());
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
