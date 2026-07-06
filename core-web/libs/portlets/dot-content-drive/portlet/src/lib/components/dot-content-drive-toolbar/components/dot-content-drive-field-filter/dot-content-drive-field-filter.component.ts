import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    linkedSignal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';

import { debounceTime } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { getSingleSelectableFieldOptions } from '@dotcms/edit-content';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DEBOUNCE_TIME,
    FIELD_FILTER_CHECKBOX_TYPE,
    FIELD_FILTER_DATE_TIME_TYPE,
    FIELD_FILTER_MULTISELECT_TYPE,
    FIELD_FILTER_RADIO_TYPE,
    FIELD_FILTER_SELECT_TYPE,
    FIELD_FILTER_TIME_ONLY_TYPE,
    PANEL_SCROLL_HEIGHT,
    USER_SEARCHABLE_PREFIX
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import { isDateFieldFilterType, serializeUserSearchableValue } from '../../../../utils/functions';

/** Which control a field renders and how its value is stored. */
type FieldFilterControl =
    | 'text'
    | 'single-select'
    | 'multi-select'
    | 'checkbox'
    | 'binary-checkbox'
    | 'radio'
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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveFieldFilterComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

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
            case FIELD_FILTER_MULTISELECT_TYPE:
                return 'multi-select';
            case FIELD_FILTER_RADIO_TYPE:
                return 'radio';
            case FIELD_FILTER_SELECT_TYPE:
                return 'single-select';
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

    /** Options for select/multi-select, parsed from `field.values`; values normalized to strings. */
    protected readonly $options = computed<FieldFilterOption[]>(() =>
        getSingleSelectableFieldOptions(this.$field().values ?? '', this.$field().dataType).map(
            (option) => ({ label: option.label, value: String(option.value) })
        )
    );

    /** value → label lookup for rendering chip summaries of select/multi-select choices. */
    readonly #labelByValue = computed(() => {
        const map = new Map<string, string>();
        for (const option of this.$options()) map.set(option.value, option.label);

        return map;
    });

    // --- Control models (derived from the raw stored value) ---

    protected readonly $textValue = linkedSignal<string>(() => this.$rawValue());

    protected readonly $selectValue = linkedSignal<string | null>(() => this.$rawValue() || null);

    protected readonly $multiValue = linkedSignal<string[]>(() => {
        const raw = this.$rawValue();

        return raw ? raw.split(',').filter(Boolean) : [];
    });

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

    /** Chip label parts: a concise summary of the current value (empty when unset). */
    protected readonly $chipSelections = computed<string[]>(() => {
        const control = this.$control();
        const raw = this.$rawValue();
        if (!raw) return [];

        // A binary checkbox shows True/False; empty is not filtering (handled above).
        if (control === 'binary-checkbox') {
            return [this.#dotMessageService.get(raw === 'true' ? 'true' : 'false')];
        }

        if (control === 'multi-select' || control === 'checkbox') {
            const labels = this.#labelByValue();

            return raw
                .split(',')
                .filter(Boolean)
                .map((value) => labels.get(value) || value);
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
