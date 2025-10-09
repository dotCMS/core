import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
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
import {
    DotDialogComponent,
    DotFieldValidationMessageComponent,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { PushPublishEnvSelectorModule } from '../dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotPushPublishFormModule } from '../forms/dot-push-publish-form/dot-push-publish-form.module';

@Component({
    selector: 'dot-push-publish-dialog',
    styleUrls: ['./dot-push-publish-dialog.component.scss'],
    templateUrl: 'dot-push-publish-dialog.component.html',
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        CalendarModule,
        DropdownModule,
        SelectButtonModule,
        DotDialogComponent,
        DotFieldValidationMessageComponent,
        DotSafeHtmlPipe,
        PushPublishEnvSelectorModule,
        DotPushPublishFormModule
    ],
    providers: [DotPushPublishFiltersService]
})
export class DotPushPublishDialogComponent implements OnInit, OnDestroy {
    private pushPublishService = inject(PushPublishService);
    private dotMessageService = inject(DotMessageService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);

    dialogActions: DotDialogActions;
    dialogShow = false;
    eventData: DotPushPublishDialogData;
    formData: DotPushPublishData;
    formValid = false;
    errorMessage = null;

    @Output() cancel = new EventEmitter<boolean>();

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
                    if (!result?.errors) {
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
