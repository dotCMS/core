import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';

import { KeyCode } from '../services/util/key-util';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-modal-dialog',
    template: `
        <p-dialog
            [style]="{ width: '900px' }"
            [header]="headerText"
            [visible]="!hidden"
            [modal]="true"
            [dismissableMask]="true"
            [closable]="false"
            [draggable]="false"
            appendTo="body">
            <p-message
                *ngIf="errorMessage"
                [text]="errorMessage"
                style="margin-bottom: 16px; display: block;"
                severity="error"></p-message>

            <ng-content></ng-content>
            <p-footer>
                <button
                    (click)="ok.emit()"
                    [label]="okButtonText"
                    [disabled]="!okEnabled"
                    type="button"
                    pButton></button>
                <button
                    (click)="cancel.emit(true)"
                    type="button"
                    pButton
                    label="Cancel"
                    class="ui-button-secondary"></button>
            </p-footer>
        </p-dialog>
    `,
    standalone: false
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
