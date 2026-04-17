import { format, isBefore, startOfDay, subDays } from 'date-fns';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    model,
    output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectButtonModule, SelectButtonChangeEvent } from 'primeng/selectbutton';

export interface CdnDateFilter {
    dateFrom: string;
    dateTo: string;
    hourly: boolean;
}

export interface CdnFilterOption {
    label: string;
    value: string;
}

export const CDN_TIME_PRESETS: CdnFilterOption[] = [
    { label: 'Today', value: 'today' },
    { label: '24h', value: 'last24h' },
    { label: '7d', value: 'last7d' },
    { label: '30d', value: 'last30d' },
    { label: '90d', value: 'last90d' },
    { label: 'Custom', value: 'custom' }
];

@Component({
    selector: 'dot-cdn-filters',
    standalone: true,
    imports: [FormsModule, SelectButtonModule, DatePickerModule, ButtonModule],
    template: `
        <div class="flex items-center gap-3">
            <p-selectbutton
                [options]="presets"
                [(ngModel)]="$selectedPreset"
                optionLabel="label"
                optionValue="value"
                [allowEmpty]="false"
                (onChange)="onPresetChange($event)"
                data-testid="cdn-period-buttons" />

            @if ($showDatePicker()) {
                <p-datepicker
                    [(ngModel)]="$customRange"
                    selectionMode="range"
                    [readonlyInput]="false"
                    [showIcon]="true"
                    [iconDisplay]="'input'"
                    placeholder="Pick start and end date"
                    dateFormat="M dd, yy"
                    [maxDate]="$today()"
                    [showButtonBar]="true"
                    [style]="{ minWidth: '280px' }"
                    (onSelect)="onDateSelect($event)"
                    (onClose)="onCalendarClosed()"
                    data-testid="cdn-custom-date-range">
                    <ng-template #buttonbar let-api>
                        <p-button
                            label="Clear"
                            [text]="true"
                            size="small"
                            (onClick)="clearDateRange(); api.onClearButtonClick($event)"
                            data-testid="cdn-date-clear-btn" />
                    </ng-template>
                </p-datepicker>
            }
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCdnFiltersComponent {
    readonly presets = CDN_TIME_PRESETS;
    readonly $today = signal<Date>(startOfDay(new Date()));
    readonly $selectedPreset = model<string>('last30d');
    readonly $customRange = model<Date[] | null>(null);
    readonly $rangeStart = signal<Date | null>(null);

    readonly $showDatePicker = computed(() => this.$selectedPreset() === 'custom');

    filterChange = output<CdnDateFilter>();

    onPresetChange(event: SelectButtonChangeEvent): void {
        const preset = event.value as string;
        if (preset === 'custom') {
            return;
        }

        this.$customRange.set(null);
        this.$rangeStart.set(null);
        this.filterChange.emit(this.resolvePreset(preset));
    }

    onDateSelect(date: Date): void {
        const start = this.$rangeStart();

        if (start === null || isBefore(date, start)) {
            this.$rangeStart.set(startOfDay(date));
        } else {
            this.$rangeStart.set(null);
            const dateFrom = format(start, 'yyyy-MM-dd');
            const dateTo = format(date, 'yyyy-MM-dd');
            const diffDays = Math.ceil(
                (date.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)
            );

            this.filterChange.emit({
                dateFrom,
                dateTo,
                hourly: diffDays <= 2
            });
        }
    }

    clearDateRange(): void {
        this.$rangeStart.set(null);
        this.$customRange.set(null);
    }

    onCalendarClosed(): void {
        const range = this.$customRange();
        if (!range || range.length !== 2) {
            this.$rangeStart.set(null);
        }
    }

    private resolvePreset(preset: string): CdnDateFilter {
        const today = format(new Date(), 'yyyy-MM-dd');

        switch (preset) {
            case 'today':
                return { dateFrom: today, dateTo: today, hourly: true };
            case 'last24h':
                return {
                    dateFrom: format(subDays(new Date(), 1), 'yyyy-MM-dd'),
                    dateTo: today,
                    hourly: true
                };
            case 'last7d':
                return {
                    dateFrom: format(subDays(new Date(), 7), 'yyyy-MM-dd'),
                    dateTo: today,
                    hourly: false
                };
            case 'last30d':
                return {
                    dateFrom: format(subDays(new Date(), 30), 'yyyy-MM-dd'),
                    dateTo: today,
                    hourly: false
                };
            case 'last90d':
                return {
                    dateFrom: format(subDays(new Date(), 90), 'yyyy-MM-dd'),
                    dateTo: today,
                    hourly: false
                };
            default:
                return {
                    dateFrom: format(subDays(new Date(), 30), 'yyyy-MM-dd'),
                    dateTo: today,
                    hourly: false
                };
        }
    }
}
