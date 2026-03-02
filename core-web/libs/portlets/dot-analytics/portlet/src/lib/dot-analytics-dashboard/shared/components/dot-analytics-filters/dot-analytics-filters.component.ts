import { signalMethod } from '@ngrx/signals';
import { addDays, format, parse, startOfDay } from 'date-fns';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    model,
    output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectChangeEvent, SelectModule } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import {
    TIME_RANGE_OPTIONS,
    TimeRange,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { FilterOption, TIME_PERIOD_OPTIONS } from '../../constants';
import {
    isValidCustomDateRange,
    MIN_CUSTOM_DATE_RANGE_DAYS
} from '../../utils/dot-analytics.utils';

/**
 * Filter controls component for analytics dashboard.
 * Currently provides time period selection with extensible design for additional filters.
 *
 */
@Component({
    selector: 'dot-analytics-filters',
    imports: [ButtonModule, DatePickerModule, SelectModule, FormsModule, DotMessagePipe],
    templateUrl: './dot-analytics-filters.component.html',
    styleUrls: ['./dot-analytics-filters.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsFiltersComponent {
    private readonly dotMessageService = inject(DotMessageService);

    /** Currently active time range value passed from the parent */
    $timeRange = input.required<TimeRangeInput>({ alias: 'timeRange' });

    /** Currently selected time period value */
    $selectedTimeRange = model<TimeRange | null>(null);

    /** Custom date range selection for calendar */
    $customDateRange = model<Date[] | null>(null);

    /** Available time period options for dropdown */
    $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Maximum selectable date — today (no future dates allowed) */
    $today = signal<Date>(startOfDay(new Date()));

    /**
     * Tracks the first date selected during range picking.
     * Used to compute which intermediate dates should be disabled.
     */
    $rangeStart = signal<Date | null>(null);

    /**
     * Dates that cannot be selected as end date after a start date is chosen.
     * Disables dates that would create a range shorter than MIN_CUSTOM_DATE_RANGE_DAYS.
     * Resets to an empty array when no start date is active.
     */
    $disabledDates = computed<Date[]>(() => {
        const start = this.$rangeStart();

        if (!start) {
            return [];
        }

        // Disable dates from start+1 to start+(MIN-2), inclusive
        // These would result in a range shorter than MIN_CUSTOM_DATE_RANGE_DAYS days
        return Array.from({ length: MIN_CUSTOM_DATE_RANGE_DAYS - 2 }, (_, i) =>
            addDays(start, i + 1)
        );
    });

    /** Check if custom time range is selected */
    $showCustomTimeRange = computed(() => this.$selectedTimeRange() === TIME_RANGE_OPTIONS.custom);

    /** Translated time period options for display */
    $translatedTimeOptions = computed(() => {
        return this.$timeOptions().map((option) => ({
            ...option,
            label: this.dotMessageService.get(option.label)
        }));
    });

    /** Emits the new time range when the user changes the filter selection */
    changeFilters = output<TimeRangeInput>();

    constructor() {
        this.#handleChangeInputTimeRange(this.$timeRange);
    }

    /** Handle change time range */
    onChangeTimeRange(event: SelectChangeEvent): void {
        if (event.value === TIME_RANGE_OPTIONS.custom) {
            return;
        }

        this.changeFilters.emit(event.value);
    }

    /**
     * Handles each date click in the calendar (range mode).
     * Tracks the first selected date to compute disabledDates for the minimum range.
     * On the second click, validates and emits the complete range.
     */
    onDateSelect(date: Date): void {
        const start = this.$rangeStart();

        if (start === null) {
            // First date click — record start for disabledDates computation
            this.$rangeStart.set(date);
        } else {
            // Second date click — range selection complete
            this.$rangeStart.set(null);
            this.onChangeCustomDateRange();
        }
    }

    /** Handle change custom date range */
    onChangeCustomDateRange(): void {
        const customDateRange = this.$customDateRange();

        if (!customDateRange?.length || customDateRange.length === 1) {
            return;
        }

        const [from, to] = customDateRange;
        const fromDate = format(from, 'yyyy-MM-dd');
        const toDate = format(to, 'yyyy-MM-dd');

        if (!isValidCustomDateRange(fromDate, toDate)) {
            return;
        }

        this.changeFilters.emit([fromDate, toDate]);
    }

    /** Clears the custom date range selection and resets the range-start tracker */
    clearDateRange(): void {
        this.$rangeStart.set(null);
        this.$customDateRange.set(null);
    }

    /**
     * Resets range-start tracker when the calendar closes without completing a range.
     * Prevents stale state on the next calendar open.
     */
    onCalendarClosed(): void {
        const range = this.$customDateRange();

        if (!range || range.length !== 2) {
            this.$rangeStart.set(null);
        }
    }

    /** Handle change input time range */
    readonly #handleChangeInputTimeRange = signalMethod<TimeRangeInput>((timeRange) => {
        this.$rangeStart.set(null);

        if (Array.isArray(timeRange)) {
            this.$selectedTimeRange.set(TIME_RANGE_OPTIONS.custom);
            const [from, to] = timeRange.map((date) => parse(date, 'yyyy-MM-dd', new Date()));
            this.$customDateRange.set([from, to]);
        } else {
            this.$selectedTimeRange.set(timeRange);
            this.$customDateRange.set(null);
        }
    });
}
