import { signalMethod } from '@ngrx/signals';

import { format } from 'date-fns';


import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    model,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { CalendarModule } from 'primeng/calendar';
import { DropdownModule, DropdownChangeEvent } from 'primeng/dropdown';

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
    imports: [CommonModule, CalendarModule, DropdownModule, FormsModule, DotMessagePipe],
    standalone: true,
    templateUrl: './dot-analytics-dashboard-filters.component.html',
    styleUrls: ['./dot-analytics-dashboard-filters.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardFiltersComponent {
    private readonly dotMessageService = inject(DotMessageService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);

    $timeRange = input.required<TimeRangeInput>({ alias: 'timeRange' });

    /** Currently selected time period value */
    readonly $selectedTimeRange = model<TimeRange | null>(null);

    /** Custom date range selection for calendar */
    readonly $customDateRange = model<Date[] | null>(null);

    /** Available time period options for dropdown */
    readonly $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Check if custom time range is selected */
    readonly $showCustomTimeRange = computed(
        () => this.$selectedTimeRange() === TIME_RANGE_OPTIONS.custom
    );

    readonly #handleChangeInputTimeRange = signalMethod<TimeRangeInput>((timeRange) => {
        if (Array.isArray(timeRange)) {
            this.$selectedTimeRange.set(TIME_RANGE_OPTIONS.custom);
            const [from, to] = timeRange.map((date) => new Date(`${date}T00:00:00.000`));
            this.$customDateRange.set([from, to]);
        } else {
            this.$selectedTimeRange.set(timeRange);
            this.$customDateRange.set(null);
        }
    });

    readonly #handleChangeCustomDateRange = signalMethod<Date[] | null>((dateRange) => {
        if (!dateRange || dateRange.length !== 2 || !dateRange[0] || !dateRange[1]) {
            return;
        }
        const customRange: DateRange = [
            dateRange[0].toISOString().split('T')[0],
            dateRange[1].toISOString().split('T')[0]
        ];

        const queryParams = this.route.snapshot.queryParamMap;

        // Read current URL params without creating dependency
        const currentUrlParams = {
            timeRange: queryParams.get('time_range'),
            from: queryParams.get('from'),
            to: queryParams.get('to')
        };

        // Convert URL time_range to internal for comparison
        const currentInternalTimeRange = currentUrlParams.timeRange
            ? fromUrlFriendly(currentUrlParams.timeRange)
            : null;

        // Only update URL if different from current values
        if (
            currentInternalTimeRange !== CUSTOM_TIME_RANGE ||
            currentUrlParams.from !== customRange[0] ||
            currentUrlParams.to !== customRange[1]
        ) {
            // Update URL with custom date range query params
            this.updateCustomDateRangeParams(customRange);
        }
    });

    constructor() {
        this.#handleChangeInputTimeRange(this.$timeRange);
    }

    /** Translated time period options for display */
    readonly $translatedTimeOptions = computed(() => {
        return this.$timeOptions().map((option) => ({
            ...option,
            label: this.dotMessageService.get(option.label)
        }));
    });

    onChangeTimeRange(event: DropdownChangeEvent): void {
        if (event.value === TIME_RANGE_OPTIONS.custom) {
            return;
        }

        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {
                time_range: event.value
            },
            queryParamsHandling: 'replace'
        });
    }

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
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {
                time_range: TIME_RANGE_OPTIONS.custom,
                from: fromDate,
                to: toDate
            },
            queryParamsHandling: 'replace'
        });
    }
}
