import { signalMethod } from '@ngrx/signals';
import { format, parse } from 'date-fns';

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

import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule, SelectChangeEvent } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import {
    TIME_RANGE_OPTIONS,
    TimeRange,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { FilterOption, TIME_PERIOD_OPTIONS } from '../../constants';
import { isValidCustomDateRange } from '../../utils/dot-analytics.utils';

/**
 * Filter controls component for analytics dashboard.
 * Currently provides time period selection with extensible design for additional filters.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-filters',
    imports: [DatePickerModule, SelectModule, FormsModule, DotMessagePipe],
    templateUrl: './dot-analytics-dashboard-filters.component.html',
    styleUrls: ['./dot-analytics-dashboard-filters.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardFiltersComponent {
    private readonly dotMessageService = inject(DotMessageService);

    $timeRange = input.required<TimeRangeInput>({ alias: 'timeRange' });

    /** Currently selected time period value */
    $selectedTimeRange = model<TimeRange | null>(null);

    /** Custom date range selection for calendar */
    $customDateRange = model<Date[] | null>(null);

    /** Available time period options for dropdown */
    $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Check if custom time range is selected */
    $showCustomTimeRange = computed(() => this.$selectedTimeRange() === TIME_RANGE_OPTIONS.custom);

    /** Translated time period options for display */
    $translatedTimeOptions = computed(() => {
        return this.$timeOptions().map((option) => ({
            ...option,
            label: this.dotMessageService.get(option.label)
        }));
    });

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

    /** Handle change input time range */
    readonly #handleChangeInputTimeRange = signalMethod<TimeRangeInput>((timeRange) => {
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
