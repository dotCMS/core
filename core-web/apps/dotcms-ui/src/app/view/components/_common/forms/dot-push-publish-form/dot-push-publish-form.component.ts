import { Observable, of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';

import { catchError, filter, map, take, takeUntil } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@dotcms/data-access';
import { DotcmsConfigService, DotTimeZone } from '@dotcms/dotcms-js';
import { DotPushPublishDialogData, DotPushPublishData } from '@dotcms/dotcms-models';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotParseHtmlService } from '../../../../../api/services/dot-parse-html/dot-parse-html.service';
import { DotFormModel } from '../../../../../shared/models/dot-form/dot-form.model';
import { PushPublishEnvSelectorComponent } from '../../dot-push-publish-env-selector/dot-push-publish-env-selector.component';

@Component({
    selector: 'dot-push-publish-form',
    templateUrl: './dot-push-publish-form.component.html',
    styleUrls: ['./dot-push-publish-form.component.scss'],
    imports: [
        CommonModule,
        AutoFocusModule,
        FormsModule,
        CalendarModule,
        PushPublishEnvSelectorComponent,
        ReactiveFormsModule,
        DropdownModule,
        DotFieldValidationMessageComponent,
        SelectButtonModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotPushPublishFormComponent
    implements OnInit, OnDestroy, DotFormModel<DotPushPublishDialogData, DotPushPublishData>
{
    private dotPushPublishFiltersService = inject(DotPushPublishFiltersService);
    private dotParseHtmlService = inject(DotParseHtmlService);
    private dotcmsConfigService = inject(DotcmsConfigService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);
    fb = inject(UntypedFormBuilder);

    readonly #dotMessageService = inject(DotMessageService);

    dateFieldMinDate = new Date();
    form: UntypedFormGroup;
    pushActions: SelectItem[];
    filterOptions: SelectItem[] = null;
    timeZoneOptions: SelectItem[] = null;
    eventData: DotPushPublishDialogData = { assetIdentifier: '', title: '' };
    assetIdentifier: string;
    localTimezone: string;
    showTimezonePicker = false;
    changeTimezoneActionLabel = this.#dotMessageService.get('Change');

    @Input() data: DotPushPublishDialogData;

    @Output() value = new EventEmitter<DotPushPublishData>();
    @Output() valid = new EventEmitter<boolean>();

    @ViewChild('customCode', { static: true }) customCodeContainer: ElementRef;

    private defaultFilterKey: string;
    private _filterOptions: SelectItem[] = null;
    private destroy$: Subject<boolean> = new Subject<boolean>();

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
            ? this.#dotMessageService.get('hide')
            : this.#dotMessageService.get('Change');
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
                this._filterOptions = filterOptions.map((item: DotPushPublishFilter) => {
                    return {
                        label: item.title,
                        value: item.key
                    };
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
                label: this.#dotMessageService.get('contenttypes.content.push_publish.action.push'),
                value: 'publish'
            },
            {
                label: this.#dotMessageService.get(
                    'contenttypes.content.push_publish.action.remove'
                ),
                value: 'expire',
                disabled: this.isRestrictedOrCategory()
            },
            {
                label: this.#dotMessageService.get(
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
