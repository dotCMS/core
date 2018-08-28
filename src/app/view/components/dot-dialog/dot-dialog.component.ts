import { Component, Input, EventEmitter, Output, ViewChild } from '@angular/core';
import { Dialog } from 'primeng/primeng';

@Component({
    selector: 'dot-dialog',
    templateUrl: './dot-dialog.component.html',
    styleUrls: ['./dot-dialog.component.scss']
})
    export class DotDialogComponent {
    @ViewChild('dialog') dialog: Dialog;

    @Input() header = '';
    @Input() show: boolean;
    @Input() ok: DotDialogAction;
    @Input() cancel: DotDialogAction;

    @Output() close: EventEmitter<any> = new EventEmitter();

    constructor() {}

    /**
     * Action when pressed Cancel button
     *
     * @memberof DotDialogComponent
     */
    cancelAction(): void {
        this.closeDialog();
        this.cancel.action(this);
    }

    /**
     * Callback when dialog hide
     *
     * @memberof DotDialogComponent
     */
    closeDialog(): void {
        this.show = false;
        this.close.emit();
    }

    /**
     * Action to re-center opened dialog displayed
     *
     * @memberof DotDialogComponent
     */
    reRecenter(): void {
        setTimeout(() => {
            this.dialog.center();
        });
    }
}

export interface DotDialogAction {
    action: (dialog: any) => void;
    disabled?: boolean;
    label: string;
}
