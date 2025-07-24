import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { TimeRange } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_TIME_PERIOD, FilterOption, TIME_PERIOD_OPTIONS } from '../../constants';
import { DateRange } from '../../types';

/**
 * Filter controls component for analytics dashboard.
 * Currently provides time period selection with extensible design for additional filters.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-filters',
    standalone: true,
    imports: [CommonModule, CalendarModule, DropdownModule, FormsModule, DotMessagePipe],
    templateUrl: './dot-analytics-dashboard-filters.component.html',
    styleUrls: ['./dot-analytics-dashboard-filters.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardFiltersComponent {
    private readonly dotMessageService = inject(DotMessageService);

    /** Currently selected time period value */
    readonly $selectedTimeRange = model<TimeRange>(DEFAULT_TIME_PERIOD);

    /** Custom date range selection for calendar */
    readonly $customDateRange = model<Date[] | null>(null);

    /** Available time period options for dropdown */
    readonly $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Emits when predefined time period selection changes */
    readonly $timeRangeChanged = output<TimeRange>({ alias: 'timeRangeChanged' });

    /** Emits when custom date range selection changes */
    readonly $customDateRangeChanged = output<DateRange>({ alias: 'customDateRangeChanged' });

    /** Check if custom time range is selected */
    readonly $showCustomTimeRange = computed(
        () => this.$selectedTimeRange() === 'CUSTOM_TIME_RANGE'
    );

    constructor() {
        // Effect to clear custom date range when switching away from custom
        effect(
            () => {
                const selectedTimeRange = this.$selectedTimeRange();

                // Clear custom date range when switching to non-custom options
                if (selectedTimeRange !== 'CUSTOM_TIME_RANGE') {
                    this.$customDateRange.set(null);
                    this.$timeRangeChanged.emit(selectedTimeRange);
                }
            },
            { allowSignalWrites: true }
        );

        // Effect to handle custom date range changes
        effect(() => {
            const dateRange = this.$customDateRange();

            if (dateRange && dateRange.length === 2 && dateRange[0] && dateRange[1]) {
                const customRange: DateRange = [
                    dateRange[0].toISOString().split('T')[0],
                    dateRange[1].toISOString().split('T')[0]
                ];
                this.$customDateRangeChanged.emit(customRange);
            }
        });
    }

    /** Translated time period options for display */
    readonly $translatedTimeOptions = computed(() => {
        return this.$timeOptions().map((option) => ({
            ...option,
            label: this.dotMessageService.get(option.label)
        }));
    });

    /**
     * Handles time period selection change from dropdown.
     * Only emits for predefined time periods, not for CUSTOM_TIME_RANGE.
     *
     * @param value - Selected time period value
     */
    onTimeRangeChange(value: TimeRange): void {
        // Only emit for predefined time periods, not for custom range
        if (value !== 'CUSTOM_TIME_RANGE') {
            this.$timeRangeChanged.emit(value);
        }
        // When CUSTOM_TIME_RANGE is selected, just show the calendar
        // The actual range will be emitted via $customDateRangeChanged when dates are selected
    }
}
