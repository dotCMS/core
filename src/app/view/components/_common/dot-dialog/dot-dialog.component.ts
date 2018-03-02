import { DotDialogService } from '../../../../api/services/dot-dialog/dot-dialog.service';
import { Component, ViewChild } from '@angular/core';
import { ConfirmDialog } from 'primeng/primeng';

@Component({
    selector: 'dot-dialog',
    templateUrl: './dot-dialog.component.html',
    styleUrls: ['./dot-dialog.component.scss']
})
export class DotDialogComponent {
    @ViewChild('cd') cd: ConfirmDialog;

    constructor(public dotDialogService: DotDialogService) {}

    /**
     * Handle confirmation dialog action button click
     *
     * @param {string} action
     * @memberof DotDialogComponent
     */
    onClickConfirm(action: string): void {
        action === 'accept' ? this.cd.accept() : this.cd.reject();
        this.dotDialogService.clearConfirm();
    }
}
