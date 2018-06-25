import { DotAlertConfirmService } from '../../../../api/services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, ViewChild } from '@angular/core';
import { ConfirmDialog } from 'primeng/primeng';

@Component({
    selector: 'dot-alert-confirm',
    templateUrl: './dot-alert-confirm.html',
    styleUrls: ['./dot-alert-confirm.scss']
})
export class DotAlertConfirmComponent {
    @ViewChild('cd') cd: ConfirmDialog;

    constructor(public dotDialogService: DotAlertConfirmService) {}

    /**
     * Handle confirmation dialog action button click
     *
     * @param {string} action
     * @memberof DotAlertConfirmComponent
     */
    onClickConfirm(action: string): void {
        action === 'accept' ? this.cd.accept() : this.cd.reject();
        this.dotDialogService.clearConfirm();
    }
}
