import { forkJoin, of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    linkedSignal,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { DatePickerModule } from 'primeng/datepicker';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';

import { catchError, debounceTime, filter, map, take, tap } from 'rxjs/operators';

import {
    DotCategoriesService,
    DotContentletService,
    DotMessageService,
    DotTagsService
} from '@dotcms/data-access';
import { DotCategory, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    DotSelectExistingContentComponent,
    getContentTypeIdFromRelationship,
    getSingleSelectableFieldOptions
} from '@dotcms/edit-content';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveRelationshipFooterComponent } from './dot-content-drive-relationship-footer/dot-content-drive-relationship-footer.component';

import {
    DEBOUNCE_TIME,
    FIELD_FILTER_CATEGORY_TYPE,
    FIELD_FILTER_CHECKBOX_TYPE,
    FIELD_FILTER_DATE_TIME_TYPE,
    FIELD_FILTER_MULTISELECT_TYPE,
    FIELD_FILTER_RADIO_TYPE,
    FIELD_FILTER_RELATIONSHIP_TYPE,
    FIELD_FILTER_SELECT_TYPE,
    FIELD_FILTER_TAG_TYPE,
    FIELD_FILTER_TIME_ONLY_TYPE,
    PANEL_SCROLL_HEIGHT,
    USER_SEARCHABLE_PREFIX,
    USER_SEARCHABLE_VALUE_SEPARATOR
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import {
    isDateFieldFilterType,
    parseMultiValue,
    serializeUserSearchableValue
} from '../../../../utils/functions';
import {
    DotContentDriveLazyMultiselectComponent,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption
} from '../dot-content-drive-lazy-multiselect/dot-content-drive-lazy-multiselect.component';

/** Which control a field renders and how its value is stored. */
type FieldFilterControl =
    | 'text'
    | 'single-select'
    | 'multi-select'
    | 'checkbox'
    | 'binary-checkbox'
    | 'radio'
    | 'lazy-multiselect'
    | 'relationship'
    | 'date';

interface FieldFilterOption {
    label: string;
    value: string;
}

/**
 * One dynamic field-based filter chip for the Content Drive toolbar. Reuses the shared
 * `dot-chip-filter` + `p-popover` pattern and renders a lean *selection* control matching the
 * content-type field type — text input, single/multi select, or a date range picker. The value is
 * stored raw in the store's flat filter bag under `us.<variable>`; the store reshapes it into the
 * `userSearchable` payload and the URL round-trip is handled by the shared encode/decode.
 */
@Component({
    selector: 'dot-content-drive-field-filter',
    imports: [
        FormsModule,
        InputTextModule,
        ListboxModule,
        RadioButtonModule,
        DatePickerModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotContentDriveLazyMultiselectComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-field-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // inline-flex so the enter/leave animation can collapse the chip's width and let neighbours
    // (the "More" button) slide smoothly instead of snapping.
    host: { class: 'inline-flex' },
    providers: [DialogService]
})
export class DotContentDriveFieldFilterComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #tagsService = inject(DotTagsService);
    readonly #categoriesService = inject(DotCategoriesService);
    readonly #contentletService = inject(DotContentletService);
    readonly #dialogService = inject(DialogService);
    readonly #destroyRef = inject(DestroyRef);

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly LISTBOX_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;
    /**
     * Flattens the inline date picker's own panel chrome (border/shadow/rounding) so it blends into
     * the chip popover as a single surface instead of a panel-inside-a-panel.
     */
    protected readonly datePickerPt = {
        panel: { class: '!border-0 !rounded-none !shadow-none' }
    };

    /** The content-type field this chip filters on. */
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Whether the popover is open. The lazy multi-select's virtual scroller must be created only
     * once the popover is visible, or it measures a zero-height viewport and renders an empty list;
     * gating the popover content on this recreates it on each open (same trick the content-type
     * filter uses).
     */
    protected readonly $popoverOpen = signal(false);

    /**
     * Debounces the store write so rapid field changes (typing, toggling options, spinning the
     * time) don't fire a search per change. The local value updates immediately; only the store
     * patch (which triggers the search) is debounced.
     */
    readonly #patch$ = new Subject<string>();

    /** Filter-bag key for this field (`us.<variable>`). */
    protected readonly $key = computed(() => `${USER_SEARCHABLE_PREFIX}${this.$field().variable}`);

    /** Raw stored value; re-reads on external changes (URL restore, clear-all, reset). */
    protected readonly $rawValue = linkedSignal<string>(() => {
        const raw = this.#store.getFilterValue(this.$key());

        return typeof raw === 'string' ? raw : '';
    });

    /** Which control to render for this field type — matched to the field's native widget. */
    protected readonly $control = computed<FieldFilterControl>(() => {
        const fieldType = this.$field().fieldType;
        if (isDateFieldFilterType(fieldType)) return 'date';

        switch (fieldType) {
            case FIELD_FILTER_CHECKBOX_TYPE:
                // A single-option checkbox is a boolean value; multiple options is a selection.
                return this.$options().length <= 1 ? 'binary-checkbox' : 'checkbox';
            case FIELD_FILTER_MULTISELECT_TYPE:
                return 'multi-select';
            // Tag and Category are unbounded, server-side lists — browsed with search + infinite
            // scroll (their options aren't defined on the field) via the shared lazy multi-select.
            case FIELD_FILTER_TAG_TYPE:
            case FIELD_FILTER_CATEGORY_TYPE:
                return 'lazy-multiselect';
            case FIELD_FILTER_RADIO_TYPE:
                return 'radio';
            case FIELD_FILTER_SELECT_TYPE:
                return 'single-select';
            case FIELD_FILTER_RELATIONSHIP_TYPE:
                return 'relationship';
            default:
                return 'text';
        }
    });

    /** Date pickers: show the time part for Date-and-Time and Time. */
    protected readonly $showTime = computed(
        () =>
            this.$field().fieldType === FIELD_FILTER_DATE_TIME_TYPE ||
            this.$field().fieldType === FIELD_FILTER_TIME_ONLY_TYPE
    );
    /** Time fields render a time-only picker. */
    protected readonly $timeOnly = computed(
        () => this.$field().fieldType === FIELD_FILTER_TIME_ONLY_TYPE
    );

    /**
     * A text field can hold plain text, a whole number, or a decimal (via its `dataType`). Surface
     * that with a matching input mode + placeholder so it's clear what to type (and mobile shows the
     * right keyboard). The value is still stored as a string.
     */
    protected readonly $textType = computed<'text' | 'integer' | 'decimal'>(() => {
        switch ((this.$field().dataType ?? '').toUpperCase()) {
            case 'INTEGER':
                return 'integer';
            case 'FLOAT':
                return 'decimal';
            default:
                return 'text';
        }
    });
    protected readonly $textInputMode = computed(() => {
        const type = this.$textType();

        return type === 'integer' ? 'numeric' : type === 'decimal' ? 'decimal' : 'text';
    });
    protected readonly $textPlaceholderKey = computed(() => {
        const type = this.$textType();

        return type === 'integer'
            ? 'content-drive.field-filter.number.placeholder'
            : type === 'decimal'
              ? 'content-drive.field-filter.decimal.placeholder'
              : 'content-drive.field-filter.text.placeholder';
    });

    /** Options for select/multi-select, parsed from `field.values`; values normalized to strings. */
    protected readonly $options = computed<FieldFilterOption[]>(() =>
        getSingleSelectableFieldOptions(this.$field().values ?? '', this.$field().dataType).map(
            (option) => ({ label: option.label, value: String(option.value) })
        )
    );

    /** value → label lookup for rendering chip summaries of option-based choices. */
    readonly #labelByValue = computed(() => {
        const map = new Map<string, string>();
        for (const option of this.$options()) map.set(option.value, option.label);

        return map;
    });

    /**
     * value → label for Tag/Category options, populated by the loader as pages come in (see
     * `$lazyLoader`). Lets the chip summary show the option name instead of the raw id — including
     * a Category inode restored from the URL, once its page loads. A value whose page hasn't loaded
     * yet falls back to showing the raw value.
     */
    readonly #lazyLabelByValue = signal<Record<string, string>>({});

    /** Records the value→label of loaded options so the chip summary can resolve ids to names. */
    #cacheLazyLabels(options: FieldFilterOption[]): void {
        if (!options.length) {
            return;
        }

        this.#lazyLabelByValue.update((cache) => {
            const next = { ...cache };
            for (const option of options) next[option.value] = option.label;

            return next;
        });
    }

    /**
     * Page loader handed to the shared lazy multi-select for Tag/Category. Tag options are keyed by
     * label (the search contract matches tags by name); Category options by inode. `hasMore` comes
     * from the response pagination when present, else falls back to a full page being returned.
     */
    protected readonly $lazyLoader = computed<DotLazyMultiselectLoader>(() => {
        const field = this.$field();

        if (field.fieldType === FIELD_FILTER_TAG_TYPE) {
            return ({ page, perPage, filter }) =>
                this.#tagsService
                    .getTagsPaginated({ page, per_page: perPage, filter, global: true })
                    .pipe(
                        map((response) => ({
                            options: (response.entity ?? []).map((tag) => ({
                                label: tag.label,
                                value: tag.label
                            })),
                            hasMore: this.#hasMore(response, page, perPage)
                        })),
                        tap((result) => this.#cacheLazyLabels(result.options))
                    );
        }

        // Category: the field's `values` holds the root category inode, so we list within that tree.
        const rootInode = field.values;

        return ({ page, perPage, filter }) => {
            const params = { page, per_page: perPage, filter };
            const request$ = rootInode
                ? this.#categoriesService.getChildrenPaginated(rootInode, params)
                : this.#categoriesService.getCategoriesPaginated(params);

            return request$.pipe(
                map((response) => ({
                    options: (response.entity ?? []).map((category) => ({
                        label: category.categoryName,
                        // Backend filters categories by inode (per the search contract).
                        value: category.inode
                    })),
                    hasMore: this.#hasMore(response, page, perPage)
                })),
                tap((result) => this.#cacheLazyLabels(result.options))
            );
        };
    });

    // --- Control models (derived from the raw stored value) ---

    protected readonly $textValue = linkedSignal<string>(() => this.$rawValue());

    protected readonly $selectValue = linkedSignal<string | null>(() => this.$rawValue() || null);

    protected readonly $multiValue = linkedSignal<string[]>(() =>
        parseMultiValue(this.$rawValue())
    );

    /**
     * A binary checkbox is a tri-state boolean filter: True / False / not-filtering. A single
     * checkbox can only express two states, so it renders as a True/False radio group instead —
     * selecting one filters for that value, and clearing (the chip X) returns to not-filtering.
     */
    protected readonly $binaryOptions = computed<FieldFilterOption[]>(() => [
        { label: this.#dotMessageService.get('true'), value: 'true' },
        { label: this.#dotMessageService.get('false'), value: 'false' }
    ]);

    /**
     * Label shown for a binary checkbox: the option's own label (e.g. "Include in Site Map") is
     * more readable than the field name/variable; falls back to the field name when unset.
     */
    protected readonly $binaryLabel = computed(
        () => this.$options()[0]?.label || this.$field().name
    );

    protected readonly $dateValue = linkedSignal<Date[] | null>(() => {
        const raw = this.$rawValue();
        if (!raw) return null;

        const dates = raw
            .split(USER_SEARCHABLE_VALUE_SEPARATOR)
            .filter(Boolean)
            .map((iso) => new Date(iso));

        return dates.length ? dates : null;
    });

    /**
     * Time-only fields use two independent pickers (from / to) instead of a range picker: PrimeNG's
     * `p-datePicker` doesn't support range selection in `timeOnly` mode, so a single range control
     * can't express a time span. Each bound is nullable so an open-ended range is allowed.
     */
    protected readonly $timeFrom = linkedSignal<Date | null>(() => {
        const [from] = this.$rawValue().split(USER_SEARCHABLE_VALUE_SEPARATOR);

        return from ? new Date(from) : null;
    });
    protected readonly $timeTo = linkedSignal<Date | null>(() => {
        const to = this.$rawValue().split(USER_SEARCHABLE_VALUE_SEPARATOR)[1];

        return to ? new Date(to) : null;
    });

    /** True when both bounds are set and `from` is after `to` — drives the inline error. */
    protected readonly $timeRangeInvalid = computed(() => {
        const from = this.$timeFrom();
        const to = this.$timeTo();
        if (!from || !to) {
            return false;
        }

        // Time-only compares by time-of-day; date-and-time by the full instant.
        return this.$timeOnly()
            ? this.#timeOfDay(from) > this.#timeOfDay(to)
            : from.getTime() > to.getTime();
    });

    protected readonly $isBinary = computed(() => this.$control() === 'binary-checkbox');

    /** Relationship is picked in a full dialog, so the chip opens it instead of a popover. */
    protected readonly $isRelationship = computed(() => this.$control() === 'relationship');
    /**
     * The picked related contentlet, keyed by identifier. The stored filter value is the identifier
     * (per the search contract); we keep the whole contentlet so we can derive its title (chip
     * label) and inode (the picker preselects by inode) without extra lookups.
     */
    readonly #relationshipItemById = signal<Record<string, DotCMSContentlet>>({});

    /** Chip label parts: a concise summary of the current value (empty when unset). */
    protected readonly $chipSelections = computed<string[]>(() => {
        const control = this.$control();
        const raw = this.$rawValue();
        if (!raw) return [];

        // A binary checkbox shows True/False; empty is not filtering (handled above).
        if (control === 'binary-checkbox') {
            return [this.#dotMessageService.get(raw === 'true' ? 'true' : 'false')];
        }

        if (control === 'relationship') {
            // Single related item: show its title when resolved, otherwise a neutral count
            // (identifiers aren't user-readable, e.g. after a cold URL restore).
            const title = this.#relationshipItemById()[raw]?.title;

            return [
                title ??
                    this.#dotMessageService.get('content-drive.field-filter.selected-count', '1')
            ];
        }

        if (control === 'multi-select' || control === 'checkbox') {
            const labels = this.#labelByValue();

            return parseMultiValue(raw).map((value) => labels.get(value) || value);
        }

        if (control === 'lazy-multiselect') {
            // Labels are cached as options are picked; a cold-restored value shows the raw value.
            const labels = this.#lazyLabelByValue();

            return parseMultiValue(raw).map((value) => labels[value] || value);
        }

        if (control === 'single-select' || control === 'radio') {
            return [this.#labelByValue().get(raw) || raw];
        }

        if (control === 'date') {
            const [from, to] = raw.split(USER_SEARCHABLE_VALUE_SEPARATOR);

            return [
                [from, to]
                    .filter(Boolean)
                    .map((iso) => this.#formatDate(iso))
                    .join(' – ')
            ];
        }

        return [raw];
    });

    protected readonly $title = computed(() => this.$field().name);

    /** Chip title: the readable option label for a binary checkbox, otherwise the field name. */
    protected readonly $chipTitle = computed(() =>
        this.$isBinary() ? this.$binaryLabel() : this.$field().name
    );

    constructor() {
        this.#patch$
            .pipe(debounceTime(DEBOUNCE_TIME), takeUntilDestroyed())
            .subscribe((raw) => this.#store.patchFilters({ [this.$key()]: raw }));

        // Resolve the related contentlet for a stored identifier we don't hold yet (cold URL
        // restore). Caching it fills the chip title and lets the picker preselect it by inode.
        // Read every signal this effect depends on up front — a guard placed before a signal read
        // would drop that signal as a dependency, so the effect wouldn't re-run when it changes.
        effect(() => {
            const control = this.$control();
            const identifier = this.$rawValue();
            const resolved = this.#relationshipItemById();

            if (control !== 'relationship' || !identifier || resolved[identifier]) {
                return;
            }

            untracked(() => this.#resolveRelationshipItem(identifier));
        });

        // Resolve stored Category inodes to their names on load (cold URL restore) so the chip
        // shows names without opening the dropdown — one request for all unresolved inodes, cached.
        // Tag needs no resolution (its stored value is the label). Signals read up front (see above).
        effect(() => {
            const control = this.$control();
            const fieldType = this.$field().fieldType;
            const raw = this.$rawValue();
            const cached = this.#lazyLabelByValue();

            if (
                control !== 'lazy-multiselect' ||
                fieldType !== FIELD_FILTER_CATEGORY_TYPE ||
                !raw
            ) {
                return;
            }

            const unresolved = parseMultiValue(raw).filter((value) => !cached[value]);
            if (!unresolved.length) {
                return;
            }

            untracked(() => this.#resolveCategoryLabels(unresolved));
        });
    }

    /** Prefetches the needed categories by inode and caches their names for the chip summary. */
    #resolveCategoryLabels(inodes: string[]): void {
        forkJoin(
            inodes.map((inode) =>
                this.#categoriesService.getCategory(inode).pipe(catchError(() => of(null)))
            )
        )
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((categories) => {
                this.#cacheLazyLabels(
                    categories
                        .filter((category): category is DotCategory => category !== null)
                        .map((category) => ({
                            value: category.inode,
                            label: category.categoryName ?? category.inode
                        }))
                );
            });
    }

    /** Fetches and caches the related contentlet by identifier (the `/content/{id}` endpoint accepts one). */
    #resolveRelationshipItem(identifier: string): void {
        this.#contentletService
            .getContentletByInode(identifier)
            .pipe(
                take(1),
                // Best-effort title resolution: on a 404 (item deleted), 500 or network error, keep
                // the chip's neutral count fallback rather than breaking — the value still filters.
                catchError(() => of(null)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((contentlet) => {
                if (contentlet) {
                    this.#relationshipItemById.update((cache) => ({
                        ...cache,
                        [identifier]: contentlet
                    }));
                }
            });
    }

    protected onTextInput(value: string): void {
        this.$textValue.set(value ?? '');
        this.#patch(value ?? '');
    }

    /**
     * Single-value listbox change (Select / Radio / binary checkbox). Clicking the selected row
     * again deselects it (value → null) → not filtering. The value is a single option string, so
     * it's stored raw.
     */
    protected onSingleValueChange(value: string | null): void {
        this.$selectValue.set(value);
        this.#patch(value ?? '');
    }

    protected onMultiChange(): void {
        this.#patch(
            serializeUserSearchableValue(this.$multiValue() ?? [], this.$field().fieldType)
        );
    }

    /**
     * Whether more pages remain after the one just returned. Prefers the response pagination
     * (`totalEntries`), falling back to "a full page came back" when pagination is absent.
     */
    #hasMore(
        response: { entity?: unknown[]; pagination?: { totalEntries?: number } },
        page: number,
        perPage: number
    ): boolean {
        const total = response.pagination?.totalEntries;
        if (typeof total === 'number') {
            return page * perPage < total;
        }

        return (response.entity?.length ?? 0) >= perPage;
    }

    /**
     * The lazy multi-select emitted a new selection (Tag/Category). The labels are already cached
     * by the loader as pages load, so just store the values (comma-joined via the shared serializer).
     */
    protected onLazySelectionChange(options: DotLazyMultiselectOption[]): void {
        this.#patch(
            serializeUserSearchableValue(
                options.map((option) => option.value),
                this.$field().fieldType
            )
        );
    }

    /**
     * Opens the reused "select existing content" dialog to pick related content. Only the target
     * content type is derived from the relationship field; the chosen contentlets' inodes become the
     * filter value. Cardinality/parent context are intentionally NOT passed — those drive the
     * edit-time "already related elsewhere" constraint that disables rows, which is irrelevant to a
     * filter. Selection is always multiple so the user can match any of several values.
     */
    protected openRelationshipDialog(): void {
        const field = this.$field();
        const contentTypeId = getContentTypeIdFromRelationship(field);
        if (!contentTypeId) {
            return;
        }

        // The stored value is the identifier, but the picker preselects a single row by inode. The
        // contentlet is held in the cache (resolved by the effect below on cold restore, or from a
        // previous pick), so read the inode from there — passing the identifier would match every
        // version of the same content.
        const identifier = this.$rawValue();
        const currentInode = identifier
            ? this.#relationshipItemById()[identifier]?.inode
            : undefined;
        const currentItemsIds = currentInode ? [currentInode] : [];

        const ref = this.#dialogService.open(DotSelectExistingContentComponent, {
            header: field.name,
            width: '90%',
            height: '90%',
            modal: true,
            appendTo: 'body',
            baseZIndex: 10000,
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field',
            style: { 'max-width': '1040px', 'max-height': '800px' },
            templates: { footer: DotContentDriveRelationshipFooterComponent },
            data: {
                contentTypeId,
                selectionMode: 'single',
                currentItemsIds
            }
        });

        ref?.onClose
            .pipe(
                filter((items): items is DotCMSContentlet[] => Array.isArray(items)),
                take(1),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((items) => {
                // Single selection → cache the whole contentlet and store its identifier (or clear).
                const [selected] = items;
                if (selected) {
                    this.#relationshipItemById.update((cache) => ({
                        ...cache,
                        [selected.identifier]: selected
                    }));
                }
                this.#patch(selected?.identifier ?? '');
            });
    }

    protected onDateChange(dates: Date[] | null): void {
        this.$dateValue.set(dates);
        const [from, to] = dates ?? [];
        const range = {
            from: from ? from.toISOString() : '',
            to: to ? to.toISOString() : ''
        };
        this.#patch(serializeUserSearchableValue(range, this.$field().fieldType));
    }

    /**
     * Updates one bound of a time-only range (from/to) independently. The bound signals keep the
     * user's input regardless of validity; the filter is only re-applied when the range is valid
     * (from ≤ to), so an inverted range shows the inline error instead of filtering wrongly.
     */
    protected onTimeBoundChange(value: Date | null, bound: 'from' | 'to'): void {
        if (bound === 'from') {
            this.$timeFrom.set(value);
        } else {
            this.$timeTo.set(value);
        }

        this.#applyRange();
    }

    /**
     * Date-and-Time uses a date-range calendar (no `showTime`, which crashes in PrimeNG's range
     * mode) plus two time-only pickers. The calendar sets the date part of each bound; the pickers
     * set the time part. Both feed the same from/to bounds.
     */
    protected onDateTimeDatesChange(dates: Date[] | null): void {
        const [fromDate, toDate] = dates ?? [];
        this.$timeFrom.set(fromDate ? this.#withDatePart(this.$timeFrom(), fromDate) : null);
        this.$timeTo.set(toDate ? this.#withDatePart(this.$timeTo(), toDate) : null);
        this.#applyRange();
    }

    /** Applies the time part to one bound of a Date-and-Time range (keeping its date). */
    protected onDateTimeTimeChange(value: Date | null, bound: 'from' | 'to'): void {
        if (bound === 'from') {
            this.$timeFrom.set(this.#withTimePart(this.$timeFrom(), value));
        } else {
            this.$timeTo.set(this.#withTimePart(this.$timeTo(), value));
        }

        this.#applyRange();
    }

    /** Serializes the current from/to bounds, unless the range is inverted (the inline error shows). */
    #applyRange(): void {
        if (this.$timeRangeInvalid()) {
            return;
        }

        const from = this.$timeFrom();
        const to = this.$timeTo();
        const range = {
            from: from ? from.toISOString() : '',
            to: to ? to.toISOString() : ''
        };
        this.#patch(serializeUserSearchableValue(range, this.$field().fieldType));
    }

    /** Copy of `base` (or today) with `date`'s year/month/day applied — keeps the time part. */
    #withDatePart(base: Date | null, date: Date): Date {
        const next = base ? new Date(base) : new Date();
        next.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());

        return next;
    }

    /** Copy of `base` (or today) with `time`'s hours/minutes/seconds applied; null time clears the bound. */
    #withTimePart(base: Date | null, time: Date | null): Date | null {
        if (!time) {
            return null;
        }

        const next = base ? new Date(base) : new Date();
        next.setHours(time.getHours(), time.getMinutes(), time.getSeconds(), 0);

        return next;
    }

    /**
     * Clears the value but keeps the chip in the toolbar — same as the built-in filters
     * (Locale/Workflow/Content-Type), whose X resets the selection without removing the chip.
     * A cleared field filter is inactive (no value → not sent in the payload).
     */
    protected onRemove(): void {
        this.#patch('');
    }

    #patch(raw: string): void {
        // Update the local value immediately (chip summary / control reflect it now); debounce the
        // store write so rapid changes don't fire a search per change.
        this.$rawValue.set(raw);
        this.#patch$.next(raw);
    }

    /** Seconds since midnight — compares time-only values without the arbitrary date component. */
    #timeOfDay(date: Date): number {
        return date.getHours() * 3600 + date.getMinutes() * 60 + date.getSeconds();
    }

    #formatDate(iso: string): string {
        const date = new Date(iso);
        if (Number.isNaN(date.getTime())) return iso;

        // 24-hour to match the pickers (which default to 24h) — avoids an AM/PM label over a 24h input.
        const time: Intl.DateTimeFormatOptions = {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        };

        if (this.$timeOnly()) {
            return date.toLocaleTimeString([], time);
        }

        if (this.$showTime()) {
            return date.toLocaleString([], {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                ...time
            });
        }

        return date.toLocaleDateString();
    }
}
