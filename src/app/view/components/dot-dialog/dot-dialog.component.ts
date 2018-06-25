import { Component, Input, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'dot-dialog',
    templateUrl: './dot-dialog.component.html',
    styleUrls: ['./dot-dialog.component.scss']
})
export class DotDialogComponent {
    @Input() header = '';
    @Input() show: boolean;
    @Input() ok: DotDialogAction;
    @Input() cancel: DotDialogAction;

    @Output() close: EventEmitter<any> = new EventEmitter();

    constructor() {}

    /**
     * Callback when dialog hide
     *
     * @memberof DotIframeDialogComponent
     */
    closeDialog(): void {
        this.show = false;
        this.close.emit();
    }
}

export interface DotDialogAction {
    label: string;
    action: () => void;
}
