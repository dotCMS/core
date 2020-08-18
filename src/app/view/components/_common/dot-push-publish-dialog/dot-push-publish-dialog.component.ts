import { Component, Output, EventEmitter } from '@angular/core';
import { OnInit, OnDestroy } from '@angular/core/src/metadata/lifecycle_hooks';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPushPublishDialogService } from 'dotcms-js';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { takeUntil } from 'rxjs/operators';

import { Subject } from 'rxjs';
import { DotPushPublishDialogData } from 'dotcms-models';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';
import { DotAjaxActionResponseView } from '@models/ajax-action-response/dot-ajax-action-response';

@Component({
    selector: 'dot-push-publish-dialog',
    styleUrls: ['./dot-push-publish-dialog.component.scss'],
    templateUrl: 'dot-push-publish-dialog.component.html'
})
export class DotPushPublishDialogComponent implements OnInit, OnDestroy {
    dialogActions: DotDialogActions;
    dialogShow = false;
    eventData: DotPushPublishDialogData;
    formData: DotPushPublishData;
    formValid = false;
    errorMessage = null;

    @Output() cancel = new EventEmitter<boolean>();

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private pushPublishService: PushPublishService,
        private dotMessageService: DotMessageService,
        private dotPushPublishDialogService: DotPushPublishDialogService
    ) {}

    ngOnInit() {
        this.dotPushPublishDialogService.showDialog$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DotPushPublishDialogData) => {
                this.showDialog(data);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close the dialog and clear the data.
     * @memberof DotPushPublishDialogComponent
     */
    close(): void {
        this.cancel.emit(true);
        this.dialogShow = false;
        this.eventData = null;
        this.errorMessage = null;
    }

    /**
     * When form is submitted
     * If form is valid then call pushPublishService with the corresponding form values
     * @memberof DotPushPublishDialogComponent
     */
    submitPushAction(): void {
        if (this.formValid) {
            this.pushPublishService
                .pushPublishContent(
                    this.eventData.assetIdentifier,
                    this.formData,
                    !!this.eventData.isBundle
                )
                .pipe(takeUntil(this.destroy$))
                .subscribe((result: DotAjaxActionResponseView) => {
                    if (!result.errors) {
                        this.close();
                    } else {
                        this.errorMessage = result.errors;
                    }
                });
        }
    }

    /**
     * Update dialog action and form validation flag.
     * @param {boolean} valid
     * @memberof DotPushPublishDialogComponent
     */
    updateFormValid(valid: boolean): void {
        this.dialogActions.accept.disabled = !valid;
        this.formValid = valid;
    }

    private showDialog(data: DotPushPublishDialogData): void {
        this.eventData = data;
        this.setDialogConfig();
        this.dialogShow = true;
    }

    private setDialogConfig(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.submitPushAction();
                },
                label: this.dotMessageService.get('contenttypes.content.push_publish.form.push'),
                disabled: !this.formValid
            },
            cancel: {
                action: () => {
                    this.close();
                },
                label: this.dotMessageService.get('contenttypes.content.push_publish.form.cancel')
            }
        };
    }
}
