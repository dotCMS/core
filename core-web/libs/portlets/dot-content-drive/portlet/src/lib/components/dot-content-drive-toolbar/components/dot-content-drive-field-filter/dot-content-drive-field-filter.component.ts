import { Subject } from 'rxjs';

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

import { debounceTime, filter, take } from 'rxjs/operators';

import { DotCategoriesService, DotMessageService, DotTagsService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
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
    USER_SEARCHABLE_PREFIX
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import {
    isDateFieldFilterType,
    parseMultiValue,
    serializeUserSearchableValue
} from '../../../../utils/functions';

/** Which control a field renders and how its value is stored. */
type FieldFilterControl =
    | 'text'
    | 'single-select'
    | 'multi-select'
    | 'checkbox'
    | 'binary-checkbox'
    | 'radio'
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

    /** Debounced free-text input so we don't fire a search per keystroke. */
    readonly #textInput$ = new Subject<string>();

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
            // Tag and Category are also multi-value browse-and-pick lists; their options are
            // fetched (see $activeOptions) rather than parsed from the field.
            case FIELD_FILTER_MULTISELECT_TYPE:
            case FIELD_FILTER_TAG_TYPE:
            case FIELD_FILTER_CATEGORY_TYPE:
                return 'multi-select';
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

    /**
     * Options fetched from a service (Tag, Category) rather than parsed from `field.values`.
     * Loaded once on init so the user can browse them instead of guessing what exists.
     */
    protected readonly $fetchedOptions = signal<FieldFilterOption[]>([]);
    protected readonly $loadingOptions = signal(false);

    /** The option list to render — fetched for Tag/Category, field-derived otherwise. */
    protected readonly $activeOptions = computed<FieldFilterOption[]>(() => {
        const fieldType = this.$field().fieldType;

        return fieldType === FIELD_FILTER_TAG_TYPE || fieldType === FIELD_FILTER_CATEGORY_TYPE
            ? this.$fetchedOptions()
            : this.$options();
    });

    /** value → label lookup for rendering chip summaries of option-based choices. */
    readonly #labelByValue = computed(() => {
        const map = new Map<string, string>();
        for (const option of this.$activeOptions()) map.set(option.value, option.label);

        return map;
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
            .split(',')
            .filter(Boolean)
            .map((iso) => new Date(iso));

        return dates.length ? dates : null;
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

        if (control === 'single-select' || control === 'radio') {
            return [this.#labelByValue().get(raw) || raw];
        }

        if (control === 'date') {
            const [from, to] = raw.split(',');

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
        this.#textInput$
            .pipe(debounceTime(DEBOUNCE_TIME), takeUntilDestroyed())
            .subscribe((value) => this.#patch(value));

        // Tag/Category options are fetched (not defined on the field), loaded once on init so the
        // user can browse the full set in the multi-select instead of typing blind.
        effect(() => {
            const field = this.$field();
            untracked(() => this.#loadFetchedOptions(field));
        });
    }

    protected onTextInput(value: string): void {
        this.$textValue.set(value ?? '');
        this.#textInput$.next(value ?? '');
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

    /** Loads the browsable option list for Tag/Category fields. */
    #loadFetchedOptions(field: DotCMSContentTypeField): void {
        if (field.fieldType === FIELD_FILTER_TAG_TYPE) {
            this.$loadingOptions.set(true);
            this.#tagsService
                .getSuggestions()
                .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
                .subscribe((tags) => {
                    this.$fetchedOptions.set(
                        tags.map((tag) => ({ label: tag.label, value: tag.label }))
                    );
                    this.$loadingOptions.set(false);
                });

            return;
        }

        if (field.fieldType === FIELD_FILTER_CATEGORY_TYPE) {
            // The field's `values` holds the root category inode, so we list within that tree.
            const rootInode = field.values;
            const params = { per_page: 1000 };
            const request$ = rootInode
                ? this.#categoriesService.getChildrenPaginated(rootInode, params)
                : this.#categoriesService.getCategoriesPaginated(params);

            this.$loadingOptions.set(true);
            request$.pipe(take(1), takeUntilDestroyed(this.#destroyRef)).subscribe((response) => {
                this.$fetchedOptions.set(
                    (response.entity ?? []).map((category) => ({
                        label: category.categoryName,
                        // Backend filters categories by inode (per the search contract).
                        value: category.inode
                    }))
                );
                this.$loadingOptions.set(false);
            });
        }
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

        // Single selection (the backend supports one related value). The stored value is the
        // identifier; the picker preselects by inode, so derive it from the cached contentlet.
        const currentInode = this.#relationshipItemById()[this.$rawValue()]?.inode;
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

        ref.onClose
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
     * Clears the value but keeps the chip in the toolbar — same as the built-in filters
     * (Locale/Workflow/Content-Type), whose X resets the selection without removing the chip.
     * A cleared field filter is inactive (no value → not sent in the payload).
     */
    protected onRemove(): void {
        this.#patch('');
    }

    #patch(raw: string): void {
        this.$rawValue.set(raw);
        this.#store.patchFilters({ [this.$key()]: raw });
    }

    #formatDate(iso: string): string {
        const date = new Date(iso);
        if (Number.isNaN(date.getTime())) return iso;

        return this.$timeOnly()
            ? date.toLocaleTimeString()
            : this.$showTime()
              ? date.toLocaleString()
              : date.toLocaleDateString();
    }
}
