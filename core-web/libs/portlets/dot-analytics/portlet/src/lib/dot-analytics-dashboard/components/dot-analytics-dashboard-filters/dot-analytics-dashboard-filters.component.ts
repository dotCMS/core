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
    signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { TimeRange } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';


import {
    CUSTOM_TIME_RANGE,
    DEFAULT_TIME_PERIOD,
    FilterOption,
    TIME_PERIOD_OPTIONS
} from '../../constants';
import { DateRange } from '../../types';
import {
    toUrlFriendly
} from '../../utils/dot-analytics.utils';


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

    // readonly $timeRange = input.required<TimeRange>({alias: 'timeRange'});

    /** Currently selected time period value */
    readonly $selectedTimeRange = model<TimeRange | null>(null);

    /** Custom date range selection for calendar */
    readonly $customDateRange = model<Date[] | null>(null);

    /** Available time period options for dropdown */
    readonly $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Check if custom time range is selected */
    readonly $showCustomTimeRange = computed(() => this.$selectedTimeRange() === CUSTOM_TIME_RANGE);

    constructor() {
        this.#handleChangePredefinedTimeRange(this.$selectedTimeRange);
        this.#handleChangeCustomDateRange(this.$customDateRange);
    }

    /** Translated time period options for display */
    readonly $translatedTimeOptions = computed(() => {
        return this.$timeOptions().map((option) => ({
            ...option,
            label: this.dotMessageService.get(option.label)
        }));
    });

    readonly #handleChangeCustomDateRange = signalMethod<Date[] | null>((dateRange) => {

        if (!dateRange || dateRange.length !== 2 || !dateRange[0] || !dateRange[1]) {
            return;
        }

        const customRange: DateRange = [
            format(dateRange[0], 'yyyy-MM-dd'),
            format(dateRange[1], 'yyyy-MM-dd')
        ];


        this.router.navigate(['/analytics/dashboard'], {
            queryParams: {
                time_range: CUSTOM_TIME_RANGE,
                from: customRange[0],
                to: customRange[1]
            },
            queryParamsHandling: 'replace',
            replaceUrl: true
        });

    });

    /**
     * Update URL parameters for predefined time ranges
     */
    readonly #handleChangePredefinedTimeRange = signalMethod<string | null>((timeRange) => {

        if (!timeRange) {
            return;
        }

        const urlFriendlyValue = toUrlFriendly(timeRange);

        console.log(urlFriendlyValue);

        /*
        this.router.navigate(['/analytics/dashboard'], {
            queryParams: {
                time_range: urlFriendlyValue,
            },
            queryParamsHandling: 'replace',
            replaceUrl: true
        });
        */
    });


}
