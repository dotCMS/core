import { Subject } from 'rxjs';

import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject, output } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';

import { takeUntil } from 'rxjs/operators';

import {
    DotMessageService,
    DotPushPublishFiltersService,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotDialogActions,
    DotPushPublishData,
    DotPushPublishDialogData
} from '@dotcms/dotcms-models';

import { DotPushPublishFormComponent } from '../forms/dot-push-publish-form/dot-push-publish-form.component';

@Component({
    selector: 'dot-push-publish-dialog',
    templateUrl: 'dot-push-publish-dialog.component.html',
    imports: [
        FormsModule,
        ReactiveFormsModule,
        DatePickerModule,
        DialogModule,
        SelectModule,
        SelectButtonModule,
        ButtonModule,
        DotPushPublishFormComponent
    ],
    providers: [DotPushPublishFiltersService]
})
export class DotPushPublishDialogComponent implements OnInit, OnDestroy {
    private pushPublishService = inject(PushPublishService);
    private dotMessageService = inject(DotMessageService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private cdr = inject(ChangeDetectorRef);

    dialogActions: DotDialogActions;
    dialogShow = false;
    eventData: DotPushPublishDialogData;
    formData: DotPushPublishData;
    formValid = false;
    errorMessage = null;
    isSaving = false;

    cancel = output<boolean>();

    private destroy$: Subject<boolean> = new Subject<boolean>();

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
        this.isSaving = false;
    }

    /**
     * When form is submitted
     * If form is valid then call pushPublishService with the corresponding form values
     * @memberof DotPushPublishDialogComponent
     */
    submitPushAction(): void {
        if (this.formValid) {
            this.isSaving = true;
            this.pushPublishService
                .pushPublishContent(
                    this.eventData.assetIdentifier,
                    this.formData,
                    !!this.eventData.isBundle
                )
                .pipe(takeUntil(this.destroy$))
                .subscribe((result: DotAjaxActionResponseView) => {
                    this.isSaving = false;
                    if (!result?.errors) {
                        this.close();
                    } else {
                        this.errorMessage = result.errors;
                    }
                    this.cdr.detectChanges();
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
