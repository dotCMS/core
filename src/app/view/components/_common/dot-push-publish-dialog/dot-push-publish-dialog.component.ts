import { Component, Input, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { OnInit, OnDestroy } from '@angular/core/src/metadata/lifecycle_hooks';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { SelectItem } from 'primeng/primeng';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { LoggerService, DotPushPublishDialogService } from 'dotcms-js';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { takeUntil, map, take, catchError } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { Subject } from 'rxjs';
import { DotPushPublishDialogData } from 'dotcms-models';
import { DotParseHtmlService } from '@services/dot-parse-html/dot-parse-html.service';
import {
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@services/dot-push-publish-filters/dot-push-publish-filters.service';

@Component({
    selector: 'dot-push-publish-dialog',
    styleUrls: ['./dot-push-publish-dialog.component.scss'],
    templateUrl: 'dot-push-publish-dialog.component.html'
})
export class DotPushPublishDialogComponent implements OnInit, OnDestroy {
    dateFieldMinDate = new Date();
    dialogActions: DotDialogActions;
    dialogShow = false;
    form: FormGroup;
    pushActions: SelectItem[];
    filterOptions: SelectItem[] = null;
    eventData: DotPushPublishDialogData = { assetIdentifier: '', title: '' };

    @Input() assetIdentifier: string;

    @Output() cancel = new EventEmitter<boolean>();

    @ViewChild('formEl') formEl: HTMLFormElement;
    @ViewChild('customCode') customCodeContainer: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();
    private defaultFilterKey: string;

    constructor(
        private pushPublishService: PushPublishService,
        public fb: FormBuilder,
        private dotMessageService: DotMessageService,
        public loggerService: LoggerService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotParseHtmlService: DotParseHtmlService,
        private dotPushPublishFiltersService: DotPushPublishFiltersService
    ) {}

    ngOnInit() {
        this.loadFilters();
        this.dotPushPublishDialogService.showDialog$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DotPushPublishDialogData) => {
                if (this.filterOptions) {
                    this.loadData(data);
                } else {
                    this.loadFilters()
                        .pipe(take(1))
                        .subscribe(() => {
                            this.loadData(data);
                        });
                }
            });
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
        this.dialogShow = false;
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
                .pushPublishContent(
                    this.assetIdentifier,
                    this.form.value,
                    !!this.eventData.isBundle
                )
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

    private loadData(data: DotPushPublishDialogData): void {
        this.eventData = data;
        if (this.eventData.customCode) {
            this.loadCustomCode();
        } else {
            this.assetIdentifier = this.eventData.assetIdentifier;
            this.pushActions = this.getPushPublishActions();
            this.initForm({
                filterKey: this.defaultFilterKey
            });
            this.setDialogConfig();
        }
        this.dialogShow = true;
    }

    private loadCustomCode(): void {
        this.dotParseHtmlService.parse(
            this.eventData.customCode,
            this.customCodeContainer.nativeElement,
            true
        );
    }

    private loadFilters(): Observable<any> {
        return this.dotPushPublishFiltersService.get().pipe(
            map((filterOptions: DotPushPublishFilter[]) => {
                this.filterOptions = filterOptions
                    .map((filter: DotPushPublishFilter) => {
                        return {
                            label: filter.title,
                            value: filter.key
                        };
                    })
                    .sort((a: SelectItem, b: SelectItem) => {
                        if (a.label > b.label) {
                            return 1;
                        }
                        if (a.label < b.label) {
                            return -1;
                        }
                        // a must be equal to b
                        return 0;
                    });

                this.defaultFilterKey = filterOptions
                    .filter(({ defaultFilter }: DotPushPublishFilter) => defaultFilter)
                    .map(({ key }: DotPushPublishFilter) => key)
                    .join();
            }),
            catchError(() => of([]))
        );
    }

    private initForm(params?: { [key: string]: any }): void {
        this.form = this.fb.group({
            ...params,
            pushActionSelected: [this.pushActions[0].value, [Validators.required]],
            publishdate: [new Date(), [Validators.required]],
            expiredate: [{ value: new Date(), disabled: true }, [Validators.required]],
            environment: ['', [Validators.required]],
            forcePush: false
        });

        const publishDate = this.form.get('publishdate');
        const expireDate = this.form.get('expiredate');
        const ppFilter = this.form.get('filterKey');

        this.form
            .get('pushActionSelected')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe((pushActionSelected: string) => {
                switch (pushActionSelected) {
                    case 'publish': {
                        publishDate.enable();
                        expireDate.disable();
                        ppFilter.enable();
                        break;
                    }
                    case 'expire': {
                        publishDate.disable();
                        expireDate.enable();
                        ppFilter.disable();
                        break;
                    }
                    default: {
                        publishDate.enable();
                        expireDate.enable();
                        ppFilter.enable();
                    }
                }
            });
    }

    private getPushPublishActions(): SelectItem[] {
        return [
            {
                label: this.dotMessageService.get('contenttypes.content.push_publish.action.push'),
                value: 'publish'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.content.push_publish.action.remove'
                ),
                value: 'expire',
                disabled: this.isRestrictedOrCategory()
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.content.push_publish.action.pushremove'
                ),
                value: 'publishexpire',
                disabled: this.eventData.removeOnly || this.isRestrictedOrCategory()
            }
        ];
    }

    private isRestrictedOrCategory(): boolean {
        return this.eventData.restricted || this.eventData.cats;
    }

    private setDialogConfig(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.submitForm();
                },
                label: this.dotMessageService.get('contenttypes.content.push_publish.form.push'),
                disabled: true
            },
            cancel: {
                action: () => {
                    this.close();
                },
                label: this.dotMessageService.get('contenttypes.content.push_publish.form.cancel')
            }
        };

        this.form.valueChanges.subscribe(() => {
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
