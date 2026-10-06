import { Observable, Subject } from 'rxjs';

import { Injectable, inject, signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotAlertConfirm } from '@dotcms/dotcms-models';

import { DotMessageService } from '../dot-messages/dot-messages.service';

/**
 * Handle global confirmation and alert dialog component
 * @export
 * @class DotAlertConfirmService
 */

@Injectable()
export class DotAlertConfirmService {
    confirmationService = inject(ConfirmationService);
    private dotMessageService = inject(DotMessageService);

    readonly alertModel = signal<DotAlertConfirm | null>(null);
    readonly confirmModel = signal<DotAlertConfirm | null>(null);
    private _confirmDialogOpened$ = new Subject<boolean>();

    /**
     * Get the confirmDialogOpened notification as an Observable
     * @returns Observable<boolean>
     */
    get confirmDialogOpened$(): Observable<boolean> {
        return this._confirmDialogOpened$.asObservable();
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     *
     * @param DotAlertConfirm dialogModel
     * @memberof DotAlertConfirmService
     */
    confirm(dialogModel: DotAlertConfirm): void {
        dialogModel.footerLabel = {
            accept: this.dotMessageService.get('dot.common.dialog.accept'),
            reject: this.dotMessageService.get('dot.common.dialog.reject'),
            ...dialogModel.footerLabel
        };

        // confirmModel drives the @if that renders <p-confirmDialog> in the host template.
        // Setting the signal and calling confirmationService.confirm() in the same microtask
        // would fail because Angular's signal-based change detection hasn't yet flushed the
        // @if block — the <p-confirmDialog> element doesn't exist in the DOM yet.
        // The setTimeout(0) defers to the next macrotask, after CD has run and the dialog
        // element is present, so PrimeNG can attach its internal focus and overlay logic.
        this.confirmModel.set(dialogModel);
        setTimeout(() => {
            this.confirmationService.confirm(dialogModel);
            this._confirmDialogOpened$.next(true);
        }, 0);
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     *
     * @param DotAlertConfirm confirmation
     * @memberof DotAlertConfirmService
     */
    alert(dialogModel: DotAlertConfirm): void {
        dialogModel.footerLabel = {
            accept: this.dotMessageService.get('dot.common.dialog.accept'),
            ...dialogModel.footerLabel
        };

        // Same deferral as confirm(): alertModel renders <p-dialog> via @if; the element
        // must exist in the DOM before subscribers act on confirmDialogOpened$.
        this.alertModel.set(dialogModel);
        setTimeout(() => {
            this._confirmDialogOpened$.next(true);
        }, 0);
    }

    /**
     * Call the alert accept action and clear the model
     *
     * @param Event $event
     * @memberof DotAlertConfirmService
     */
    alertAccept($event?: Event): void {
        const model = this.alertModel();
        if (model?.accept) {
            model.accept($event);
        }

        this.alertModel.set(null);
    }

    /**
     * Call the alert reject action and clear the model
     *
     * @memberof DotAlertConfirmService
     */
    alertReject($event: Event): void {
        const model = this.alertModel();
        if (model?.reject) {
            model.reject($event);
        }

        this.alertModel.set(null);
    }

    /**
     * clear confirm dialog object
     *
     * @memberof DotAlertConfirmService
     */
    clearConfirm(): void {
        this.confirmModel.set(null);
    }
}
