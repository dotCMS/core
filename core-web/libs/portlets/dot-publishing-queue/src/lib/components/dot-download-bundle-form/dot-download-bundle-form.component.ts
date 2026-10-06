import { Subject, of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    OnInit,
    inject,
    input,
    output
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';

import { catchError, map, take, takeUntil } from 'rxjs/operators';

import {
    DotMessageService,
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

/** Value shape emitted by this form. `operation` uses the BE vocabulary that
 * `/api/bundle/_generate` expects — `'0'` = publish, `'1'` = unpublish. */
export interface DotDownloadBundleFormValue {
    bundleId: string;
    operation: '0' | '1';
    filterKey: string;
}

enum DownloadType {
    PUBLISH = 'publish',
    UNPUBLISH = 'unpublish'
}

/**
 * Presentational form used both by the legacy `DotDownloadBundleDialogComponent`
 * (in a modal) and by the Select Bundle dialog's inline "Download" step.
 *
 * Handles: filter loading, form state, Publish/Unpublish toggle, and enabling
 * or disabling the filter dropdown accordingly. Emits value + validity so the
 * parent can drive the submit button and the HTTP call — the form itself does
 * NOT submit or download.
 */
@Component({
    selector: 'dot-download-bundle-form',
    templateUrl: './dot-download-bundle-form.component.html',
    imports: [
        ReactiveFormsModule,
        SelectModule,
        SelectButtonModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDownloadBundleFormComponent implements OnInit, OnDestroy {
    private readonly fb = inject(FormBuilder);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotPushPublishFiltersService = inject(DotPushPublishFiltersService);

    readonly bundleId = input<string>('');
    readonly value = output<DotDownloadBundleFormValue>();
    readonly valid = output<boolean>();

    form!: FormGroup;
    downloadOptions: SelectItem[] = [];
    filterOptions: SelectItem[] = [];

    private defaultFilterKey = '';
    private readonly destroy$ = new Subject<boolean>();

    ngOnInit(): void {
        this.downloadOptions = [
            {
                label: this.dotMessageService.get('download.bundle.publish'),
                value: DownloadType.PUBLISH
            },
            {
                label: this.dotMessageService.get('download.bundle.unPublish'),
                value: DownloadType.UNPUBLISH
            }
        ];

        this.form = this.fb.group({
            downloadOptionSelected: [DownloadType.PUBLISH, [Validators.required]],
            filterKey: [''],
            bundleId: [this.bundleId()]
        });

        this.loadFilters();
        this.listenForChanges();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private loadFilters(): void {
        this.dotPushPublishFiltersService
            .get()
            .pipe(
                take(1),
                map((filters: DotPushPublishFilter[]) => {
                    this.defaultFilterKey =
                        filters.find((filter) => filter.defaultFilter)?.key ?? '';
                    return filters
                        .map((filter) => ({ label: filter.title, value: filter.key }))
                        .sort((a, b) => a.label.localeCompare(b.label));
                }),
                catchError(() => of([] as SelectItem[]))
            )
            .subscribe((options) => {
                this.filterOptions = options;
                this.form.patchValue({ filterKey: this.defaultFilterKey });
                // patchValue above emits a valueChange, which will notify the
                // parent through `listenForChanges`.
            });
    }

    private listenForChanges(): void {
        this.form
            .get('downloadOptionSelected')
            ?.valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe((state: DownloadType) => this.applyDownloadTypeToggle(state));

        this.form.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => this.emitCurrentValue());

        this.form.statusChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => this.valid.emit(this.form.valid));

        // Emit the initial state so parents don't wait for a user interaction
        // to know whether the form is valid.
        this.emitCurrentValue();
        this.valid.emit(this.form.valid);
    }

    private applyDownloadTypeToggle(state: DownloadType): void {
        const filterKey = this.form.get('filterKey');
        if (!filterKey) {
            return;
        }
        if (state === DownloadType.UNPUBLISH) {
            filterKey.disable({ emitEvent: false });
            filterKey.setValue('', { emitEvent: false });
            this.filterOptions = [];
        } else {
            filterKey.enable({ emitEvent: false });
            filterKey.setValue(this.defaultFilterKey, { emitEvent: false });
            // Restore the filter list — reloading is unnecessary since the
            // response was cached in memory on ngOnInit.
            this.loadFilters();
        }
        this.emitCurrentValue();
    }

    private emitCurrentValue(): void {
        const raw = this.form.getRawValue();
        this.value.emit({
            bundleId: raw.bundleId ?? this.bundleId(),
            operation: raw.downloadOptionSelected === DownloadType.PUBLISH ? '0' : '1',
            filterKey: raw.filterKey ?? ''
        });
    }
}
