import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    model,
    output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { TimeRange } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_TIME_PERIOD, FilterOption, TIME_PERIOD_OPTIONS } from '../../constants';

/**
 * Filter controls component for analytics dashboard.
 * Currently provides time period selection with extensible design for additional filters.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-filters',
    standalone: true,
    imports: [CommonModule, DropdownModule, FormsModule, DotMessagePipe],
    templateUrl: './dot-analytics-dashboard-filters.component.html',
    styleUrls: ['./dot-analytics-dashboard-filters.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardFiltersComponent {

    private readonly dotMessageService = inject(DotMessageService);

    /** Currently selected time period value */
    readonly $selectedTimeRange = model<string>(DEFAULT_TIME_PERIOD);

    /** Available time period options for dropdown */
    readonly $timeOptions = signal<FilterOption[]>(TIME_PERIOD_OPTIONS);

    /** Emits when time period selection changes */
    readonly $timeRangeChanged = output<TimeRange>({ alias: 'timeRangeChanged' });

    readonly $selected = input.required<TimeRange>({ alias: 'selectedTimeRange' });

    constructor() {
        effect(() => {
            const selectedTimeRange = this.$selected();
            this.$selectedTimeRange.set(selectedTimeRange);
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
     * Handles time period selection change.
     * Updates internal state and emits change event.
     *
     * @param value - Selected time period value
     */
    onTimeRangeChange(value: TimeRange): void {
        this.$timeRangeChanged.emit(value);
    }
}
