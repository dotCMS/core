import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { KeyCode } from '../services/util/key-util';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-modal-dialog',
    template: `<p-dialog
        [style]="{ width: '900px' }"
        [header]="headerText"
        [visible]="!hidden"
        [modal]="true"
        [dismissableMask]="true"
        [closable]="false"
        appendTo="body"
        [draggable]="false"
    >
        <p-message
            *ngIf="errorMessage"
            style="margin-bottom: 16px; display: block;"
            severity="error"
            [text]="errorMessage"
        ></p-message>

        <ng-content></ng-content>
        <p-footer>
            <button
                type="button"
                pButton
                (click)="ok.emit()"
                [label]="okButtonText"
                [disabled]="!okEnabled"
            ></button>
            <button
                type="button"
                pButton
                (click)="cancel.emit(true)"
                label="Cancel"
                class="ui-button-secondary"
            ></button>
        </p-footer>
    </p-dialog> `
})
export class ModalDialogComponent {
    @Input() okEnabled = true;
    @Input() hidden = true;
    @Input() headerText = '';
    @Input() okButtonText = 'Ok';
    @Input() errorMessage: string = null;

    @Output() close: EventEmitter<{ isCanceled: boolean }> = new EventEmitter(false);
    @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);
    @Output() ok: EventEmitter<boolean> = new EventEmitter(false);
    @Output() open: EventEmitter<boolean> = new EventEmitter(false);

    private _keyListener: any;

    constructor() {}

    onCancel(_e): void {
        this.cancel.emit(true);
    }

    ngOnChanges(change): void {
        if (change.hidden) {
            if (!this.hidden) {
                this.addEscapeListener();

                // wait until the dialog is really show up
                setTimeout(() => this.open.emit(false), 2);
            } else {
                this.removeEscapeListener();
            }
        }
    }

    private addEscapeListener(): void {
        if (!this._keyListener) {
            this._keyListener = (e) => {
                if (e.keyCode === KeyCode.ESCAPE) {
                    e.preventDefault();
                    e.stopPropagation();
                    this.cancel.emit(false);
                } else if (e.keyCode === KeyCode.ENTER) {
                    e.stopPropagation();
                    e.preventDefault();
                    this.ok.emit(true);
                }
            };
            document.body.addEventListener('keyup', this._keyListener);
        }
    }

    private removeEscapeListener(): void {
        if (this._keyListener) {
            document.body.removeEventListener('keyup', this._keyListener);
            this._keyListener = null;
        }
    }
}
