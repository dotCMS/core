import { Component, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { OnInit, OnDestroy } from '@angular/core/src/metadata/lifecycle_hooks';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { SelectItem } from 'primeng/primeng';
import { DotMessageService } from '@services/dot-messages-service';
import { LoggerService } from 'dotcms-js';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { takeUntil, map } from 'rxjs/operators';
import { combineLatest, Observable } from 'rxjs';
import { Subject } from 'rxjs';
import {
    DotPushPublishFiltersService,
    DotPushPublishFilter
} from '@services/dot-push-publish-filters/dot-push-publish-filters.service';

@Component({
    selector: 'dot-push-publish-dialog',
    styleUrls: ['./dot-push-publish-dialog.component.scss'],
    templateUrl: 'dot-push-publish-dialog.component.html'
})
export class DotPushPublishContentTypesDialogComponent implements OnInit, OnDestroy {
    dateFieldMinDate = new Date();
    dialogActions: DotDialogActions;
    dialogShow = false;
    form: FormGroup;
    pushActions: SelectItem[];
    filterOptions: SelectItem[];

    isPushActionPublish$: Observable<boolean>;
    isPushActionExpire$: Observable<boolean>;

    @Input()
    assetIdentifier: string;

    @Output()
    cancel = new EventEmitter<boolean>();

    @ViewChild('formEl')
    formEl: HTMLFormElement;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private pushPublishService: PushPublishService,
        public fb: FormBuilder,
        public dotMessageService: DotMessageService,
        public loggerService: LoggerService,
        private dotPushPublishFiltersService: DotPushPublishFiltersService
    ) {}

    ngOnInit() {
        const messages$ = this.dotMessageService.getMessages([
            'contenttypes.content.push_publish',
            'contenttypes.content.push_publish.filters',
            'contenttypes.content.push_publish.action.push',
            'contenttypes.content.push_publish.action.remove',
            'contenttypes.content.push_publish.action.pushremove',
            'contenttypes.content.push_publish.I_want_To',
            'contenttypes.content.push_publish.force_push',
            'contenttypes.content.push_publish.publish_date',
            'contenttypes.content.push_publish.expire_date',
            'contenttypes.content.push_publish.push_to',
            'contenttypes.content.push_publish.push_to_errormsg',
            'contenttypes.content.push_publish.form.cancel',
            'contenttypes.content.push_publish.form.push',
            'contenttypes.content.push_publish.publish_date_errormsg',
            'contenttypes.content.push_publish.expire_date_errormsg'
        ]);
        const filterOptions$ = this.dotPushPublishFiltersService.get();

        combineLatest(messages$, filterOptions$)
            .pipe(takeUntil(this.destroy$))
            .subscribe(
                ([messages, filterOptions]: [
                    { [key: string]: string },
                    DotPushPublishFilter[]
                ]) => {
                    this.filterOptions = filterOptions.map((filter: DotPushPublishFilter) => {
                        return {
                            label: filter.title,
                            value: filter.key
                        };
                    });

                    const defaultFilterKey = filterOptions
                        .filter((filter: DotPushPublishFilter) => filter.default)
                        .map(({ key }: DotPushPublishFilter) => key)
                        .join();

                    this.pushActions = this.getPushPublishActions(messages);
                    this.initForm({
                        filterKey: defaultFilterKey
                    });
                    this.setDialogConfig(messages, this.form);
                    this.dialogShow = true;
                }
            );
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close the dialog and reset the form
     * @memberof PushPublishContentTypesDialogComponent
     */
    close(): void {
        this.cancel.emit(true);
        this.initForm();
    }

    /**
     * When form is submitted
     * If form is valid then call pushPublishService with contentTypeId and form value params
     * @param any $event
     * @memberof PushPublishContentTypesDialogComponent
     */
    submitPushAction(_event): void {
        if (this.form.valid) {
            this.pushPublishService
                .pushPublishContent(this.assetIdentifier, this.form.value)
                .subscribe((result: any) => {
                    if (!result.errors) {
                        this.close();
                    } else {
                        this.loggerService.debug(result.errorMessages);
                    }
                });
            this.form.reset();
        }
    }

    /**
     * It submits the form from submit button
     * @memberof PushPublishContentTypesDialogComponent
     */
    submitForm(): void {
        this.formEl.ngSubmit.emit();
    }

    private initForm(params?: { [key: string]: any }): void {
        this.form = this.fb.group({
            ...params,
            pushActionSelected: [this.pushActions[0].value || '', [Validators.required]],
            publishdate: [new Date(), [Validators.required]],
            expiredate: [new Date(), [Validators.required]],
            environment: ['', [Validators.required]],
            forcePush: false
        });

        this.isPushActionPublish$ = this.form.valueChanges.pipe(
            map(
                ({ pushActionSelected }) =>
                    pushActionSelected === 'publish' || pushActionSelected === 'publishexpire'
            )
        );

        this.isPushActionExpire$ = this.form.valueChanges.pipe(
            map(
                ({ pushActionSelected }) =>
                    pushActionSelected === 'expire' || pushActionSelected === 'publishexpire'
            )
        );
    }

    private getPushPublishActions(messages: { [key: string]: string }): SelectItem[] {
        return [
            {
                label: messages['contenttypes.content.push_publish.action.push'],
                value: 'publish'
            },
            {
                label: messages['contenttypes.content.push_publish.action.remove'],
                value: 'expire'
            },
            {
                label: messages['contenttypes.content.push_publish.action.pushremove'],
                value: 'publishexpire'
            }
        ];
    }

    private setDialogConfig(messages: { [key: string]: string }, form: FormGroup): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.submitForm();
                },
                label: messages['contenttypes.content.push_publish.form.push'],
                disabled: true
            },
            cancel: {
                action: () => {
                    this.close();
                },
                label: messages['contenttypes.content.push_publish.form.cancel']
            }
        };

        form.valueChanges.subscribe(() => {
            this.dialogActions = {
                ...this.dialogActions,
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid
                }
            };
        });
    }
}
