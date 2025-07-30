import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    signal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

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
    fromUrlFriendly,
    isValidCustomDateRange,
    isValidTimeRange,
    toUrlFriendly
} from '../../utils/dot-analytics.utils';

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
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);

    /** Currently selected time period value */
    readonly $selectedTimeRange = model<TimeRange>(DEFAULT_TIME_PERIOD);

    /** Custom date range selection for calendar */
    readonly $customDateRange = model<Date[] | null>(null);

    /** Available time period options for dropdown */
    readonly $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Check if custom time range is selected */
    readonly $showCustomTimeRange = computed(() => this.$selectedTimeRange() === CUSTOM_TIME_RANGE);

    constructor() {
        // Initialize from URL params
        this.initFromUrl();

        // Clear custom date range when switching away from custom
        effect(
            () => {
                const selectedTimeRange = this.$selectedTimeRange();

                // Clear custom date range when switching to non-custom options
                if (selectedTimeRange !== CUSTOM_TIME_RANGE) {
                    this.$customDateRange.set(null);
                }
            }
        );

        // Synchronize filter changes to URL (avoiding infinite loop)
        effect(() => {
            const selectedTimeRange = this.$selectedTimeRange();

            // Read current URL param without creating dependency
            const currentUrlTimeRange = untracked(() => {
                return this.route.snapshot.queryParams['time_range'];
            });

            // Convert current URL value to internal for comparison
            const currentInternalTimeRange = currentUrlTimeRange
                ? fromUrlFriendly(currentUrlTimeRange)
                : null;

            // Only update URL if different from current value
            if (
                selectedTimeRange !== CUSTOM_TIME_RANGE &&
                selectedTimeRange.toString() !== currentInternalTimeRange
            ) {
                // For predefined ranges, clear custom date params and set time_range
                this.updateUrlParams(selectedTimeRange.toString());
            }
        });

        // Handle custom date range changes and emit formatted dates
        effect(() => {
            const dateRange = this.$customDateRange();

            if (dateRange && dateRange.length === 2 && dateRange[0] && dateRange[1]) {
                const customRange: DateRange = [
                    dateRange[0].toISOString().split('T')[0],
                    dateRange[1].toISOString().split('T')[0]
                ];

                // Read current URL params without creating dependency
                const currentUrlParams = untracked(() => {
                    const params = this.route.snapshot.queryParams;

                    return {
                        timeRange: params['time_range'],
                        from: params['from'],
                        to: params['to']
                    };
                });

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
     * The effect will handle URL synchronization and emission automatically.
     *
     * @param value - Selected time period value
     */
    onTimeRangeChange(value: TimeRange): void {
        this.$selectedTimeRange.set(value);
    }

    /**
     * Initialize filter state from URL parameters
     */
    private initFromUrl(): void {
        const params = this.route.snapshot.queryParams;
        const urlTimeRange = params['time_range'];
        const fromDate = params['from'];
        const toDate = params['to'];

        if (urlTimeRange) {
            // Convert URL-friendly value to internal value
            const internalTimeRange = fromUrlFriendly(urlTimeRange);

            // Check if it's a custom date range
            if (internalTimeRange === CUSTOM_TIME_RANGE && fromDate && toDate) {
                // Validate the custom date range
                if (isValidCustomDateRange(fromDate, toDate)) {
                    this.$selectedTimeRange.set(CUSTOM_TIME_RANGE);
                    this.$customDateRange.set([new Date(fromDate), new Date(toDate)]);
                } else {
                    // Invalid dates - fall back to default
                    this.setDefaultTimeRange();
                }
            }
            // Otherwise check for predefined time range (but not CUSTOM_TIME_RANGE without dates)
            else if (
                internalTimeRange !== CUSTOM_TIME_RANGE &&
                isValidTimeRange(internalTimeRange)
            ) {
                this.$selectedTimeRange.set(internalTimeRange as TimeRange);
            } else {
                // Invalid time range - fall back to default
                this.setDefaultTimeRange();
            }
        }
    }

    /**
     * Update URL parameters for predefined time ranges
     */
    private updateUrlParams(timeRange: string): void {
        const urlFriendlyValue = toUrlFriendly(timeRange);

        this.router.navigate([], {
            queryParams: {
                time_range: urlFriendlyValue,
                from: null, // Clear custom date params
                to: null // Clear custom date params
            },
            queryParamsHandling: 'merge',
            replaceUrl: true
        });
    }

    /**
     * Update URL parameters for custom date range
     */
    private updateCustomDateRangeParams(dateRange: DateRange): void {
        const urlFriendlyCustom = toUrlFriendly(CUSTOM_TIME_RANGE);

        this.router.navigate([], {
            queryParams: {
                time_range: urlFriendlyCustom, // Set to URL-friendly custom value
                from: dateRange[0], // Set start date
                to: dateRange[1] // Set end date
            },
            queryParamsHandling: 'merge',
            replaceUrl: true // Don't add to browser history
        });
    }

    /**
     * Set default time range values when URL parameters are invalid
     */
    private setDefaultTimeRange(): void {
        this.$selectedTimeRange.set(DEFAULT_TIME_PERIOD);
        this.$customDateRange.set(null);

        // Clear invalid query parameters and set default
        this.updateUrlParams(DEFAULT_TIME_PERIOD);
    }
}
