import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    OnDestroy
} from '@angular/core';
import {
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@services/dot-push-publish-filters/dot-push-publish-filters.service';
import { catchError, filter, map, take, takeUntil } from 'rxjs/operators';
import { DotPushPublishDialogData } from '@dotcms/dotcms-models';
import { Observable, of, Subject } from 'rxjs';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DotParseHtmlService } from '@services/dot-parse-html/dot-parse-html.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';
import { SelectItem } from 'primeng/api';
import { DotFormModel } from '@models/dot-form/dot-form.model';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotcmsConfigService, DotTimeZone } from '@dotcms/dotcms-js';

@Component({
    selector: 'dot-push-publish-form',
    templateUrl: './dot-push-publish-form.component.html',
    styleUrls: ['./dot-push-publish-form.component.scss']
})
export class DotPushPublishFormComponent
    implements OnInit, OnDestroy, DotFormModel<DotPushPublishDialogData, DotPushPublishData>
{
    dateFieldMinDate = new Date();
    form: UntypedFormGroup;
    pushActions: SelectItem[];
    filterOptions: SelectItem[] = null;
    timeZoneOptions: SelectItem[] = null;
    eventData: DotPushPublishDialogData = { assetIdentifier: '', title: '' };
    assetIdentifier: string;
    localTimezone: string;
    showTimezonePicker = false;
    changeTimezoneActionLabel = this.dotMessageService.get('Change');

    @Input() data: DotPushPublishDialogData;

    @Output() value = new EventEmitter<DotPushPublishData>();
    @Output() valid = new EventEmitter<boolean>();

    @ViewChild('customCode', { static: true }) customCodeContainer: ElementRef;

    private defaultFilterKey: string;
    private _filterOptions: SelectItem[] = null;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotPushPublishFiltersService: DotPushPublishFiltersService,
        private dotParseHtmlService: DotParseHtmlService,
        private dotMessageService: DotMessageService,
        private dotcmsConfigService: DotcmsConfigService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        public fb: UntypedFormBuilder
    ) {}

    ngOnInit() {
        if (this.data) {
            this.setPreviousDayToMinDate();
            if (this.filterOptions) {
                this.loadData(this.data);
            } else {
                this.loadFilters()
                    .pipe(take(1))
                    .subscribe(() => {
                        this.loadData(this.data);
                    });
            }
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Emit if form is valid and the value.
     * @memberof DotPushPublishFormComponent
     */
    emitValues(): void {
        this.valid.emit(this.form.valid);
        this.value.emit(this.form.value);
    }

    /**
     * Changes timezone label according to the one picked at the timezone dropdown
     * @param string timezone
     * @memberof DotPushPublishFormComponent
     */
    updateTimezoneLabel(timezone: string): void {
        this.localTimezone = this.timeZoneOptions.find(({ value }) => value === timezone)['label'];
    }

    /**
     * Show/Hide timezone dropdown picker and changes link label
     * @param MouseEvent event
     * @memberof DotPushPublishFormComponent
     */
    toggleTimezonePicker(event: MouseEvent): void {
        event.preventDefault();
        this.showTimezonePicker = !this.showTimezonePicker;

        this.changeTimezoneActionLabel = this.showTimezonePicker
            ? this.dotMessageService.get('hide')
            : this.dotMessageService.get('Change');
    }

    private setPreviousDayToMinDate() {
        const today = new Date();
        this.dateFieldMinDate.setDate(today.getDate() - 1);
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
            this.form.valueChanges.subscribe(() => {
                this.emitValues();
            });
            this.emitValues();
        }
        this.loadTimezones();
    }

    private loadCustomCode(): void {
        this.dotParseHtmlService.parse(
            this.eventData.customCode,
            this.customCodeContainer.nativeElement,
            true
        );
    }

    private setUsersTimeZone(): void {
        const ppTimezone = this.form.get('timezoneId');

        const localTZItem = this.timeZoneOptions.find(
            ({ value }) => value === Intl.DateTimeFormat().resolvedOptions().timeZone
        );
        ppTimezone.setValue(localTZItem.value);
        this.localTimezone = localTZItem.label;
    }

    private loadTimezones(): void {
        this.dotcmsConfigService
            .getTimeZones()
            .pipe(take(1))
            .subscribe((timezones: DotTimeZone[]) => {
                this.timeZoneOptions = timezones.map((item: DotTimeZone) => {
                    return {
                        label: item.label,
                        value: item.id
                    };
                });
                this.setUsersTimeZone();
            });
    }

    private loadFilters(): Observable<unknown> {
        return this.dotPushPublishFiltersService.get().pipe(
            map((filterOptions: DotPushPublishFilter[]) => {
                this._filterOptions = filterOptions
                    .map((item: DotPushPublishFilter) => {
                        return {
                            label: item.title,
                            value: item.key
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

                this.filterOptions = this._filterOptions;

                this.defaultFilterKey = filterOptions
                    .filter(({ defaultFilter }: DotPushPublishFilter) => defaultFilter)
                    .map(({ key }: DotPushPublishFilter) => key)
                    .join();
            }),
            catchError((error) => {
                this.httpErrorManagerService.handle(error);
                return of([]);
            })
        );
    }

    private initForm(params?: { [key: string]: string }): void {
        this.form = this.fb.group({
            ...params,
            pushActionSelected: [this.pushActions[0].value, [Validators.required]],
            publishDate: [new Date(), [Validators.required]],
            expireDate: [{ value: new Date(), disabled: true }, [Validators.required]],
            timezoneId: [''],
            environment: ['', [Validators.required]]
        });

        const publishDate = this.form.get('publishDate');
        const expireDate = this.form.get('expireDate');
        const ppFilter = this.form.get('filterKey');

        const enableFilters = () => {
            ppFilter.enable();
            this.filterOptions = this._filterOptions;
            ppFilter.setValue(this.defaultFilterKey);
        };

        this.form
            .get('filterKey')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .pipe(filter((value: string) => !!value))
            .subscribe((filterSelected: string) => {
                this.defaultFilterKey = filterSelected;
            });

        this.form
            .get('pushActionSelected')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe((pushActionSelected: string) => {
                switch (pushActionSelected) {
                    case 'publish': {
                        publishDate.enable();
                        expireDate.disable();
                        enableFilters();
                        break;
                    }
                    case 'expire': {
                        publishDate.disable();
                        expireDate.enable();
                        ppFilter.disable();
                        ppFilter.setValue('');
                        this.filterOptions = [];
                        break;
                    }
                    default: {
                        publishDate.enable();
                        expireDate.enable();
                        enableFilters();
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
                disabled: !!this.eventData.removeOnly || this.isRestrictedOrCategory()
            }
        ];
    }

    private isRestrictedOrCategory(): boolean {
        return !!(this.eventData.restricted || this.eventData.cats);
    }
}
