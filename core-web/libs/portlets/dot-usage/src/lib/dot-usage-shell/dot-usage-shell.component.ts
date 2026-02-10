import { Subscription } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    inject,
    OnInit,
    OnDestroy,
    computed,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessagesModule } from 'primeng/messages';
import { SkeletonModule } from 'primeng/skeleton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotUsageService, MetricData, UsageSummary } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'lib-dot-usage-shell',
    imports: [
        CommonModule,
        ButtonModule,
        CardModule,
        MessagesModule,
        SkeletonModule,
        DotMessagePipe,
        ToolbarModule
    ],
    templateUrl: './dot-usage-shell.component.html',
    styleUrl: './dot-usage-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUsageShellComponent implements OnInit, OnDestroy {
    private readonly usageService = inject(DotUsageService);
    private dataSubscription?: Subscription;

    // UI state managed by component
    readonly summary = signal<UsageSummary | null>(null);
    readonly loading = signal<boolean>(false);
    readonly error = signal<string | null>(null);
    readonly errorStatus = signal<number | null>(null);

    // Computed values for display
    readonly hasData = computed(() => this.summary() !== null);
    readonly lastUpdated = signal<Date | null>(null);

    ngOnInit(): void {
        this.loadData();
    }

    ngOnDestroy(): void {
        if (this.dataSubscription) {
            this.dataSubscription.unsubscribe();
        }
    }

    loadData(): void {
        // Unsubscribe from previous subscription if it exists
        if (this.dataSubscription) {
            this.dataSubscription.unsubscribe();
        }

        this.loading.set(true);
        this.error.set(null);
        this.errorStatus.set(null);

        this.dataSubscription = this.usageService.getSummary().subscribe({
            next: (summary) => {
                this.summary.set(summary);
                this.loading.set(false);
                this.lastUpdated.set(new Date());
            },
            error: (error) => {
                const errorMessage = this.usageService.getErrorMessage(error);
                this.error.set(errorMessage);
                this.errorStatus.set(error.status || null);
                this.loading.set(false);
                console.error('Failed to load usage data:', error);
            }
        });
    }

    onRetry(): void {
        this.summary.set(null);
        this.error.set(null);
        this.errorStatus.set(null);
        this.loadData();
    }

    formatNumber(num: number | string | undefined): string {
        if (num === undefined || num === null) {
            return '0';
        }
        const numValue = typeof num === 'string' ? parseFloat(num) : num;
        if (isNaN(numValue)) {
            return String(num);
        }
        if (numValue >= 1000000) {
            return (numValue / 1000000).toFixed(1) + 'M';
        }
        if (numValue >= 1000) {
            return (numValue / 1000).toFixed(1) + 'K';
        }
        return numValue.toLocaleString();
    }

    /**
     * Formats a metric value for display, handling both numeric and string values.
     */
    formatMetricValue(value: number | string | undefined | null): string {
        if (value === undefined || value === null) {
            return 'usage.dashboard.value.notAvailable';
        }

        // If it's already a string, check if it's a number string
        if (typeof value === 'string') {
            const numValue = parseFloat(value);
            if (!isNaN(numValue)) {
                // It's a numeric string, format it as a number
                return this.formatNumber(numValue);
            }
            // It's a non-numeric string, return as-is
            return value || 'usage.dashboard.value.notAvailable';
        }

        // It's a number, format it
        return this.formatNumber(value);
    }

    /**
     * Checks if a value should be displayed as text (non-numeric string).
     */
    isStringValue(value: number | string | undefined | null): boolean {
        if (value === undefined || value === null) {
            return false;
        }
        if (typeof value === 'string') {
            // Check if it's a numeric string
            const numValue = parseFloat(value);
            return isNaN(numValue);
        }
        return false;
    }

    /**
     * Gets metric data (including display label) by category and metric name.
     * Returns undefined if the metric is not available (e.g., not in current profile).
     */
    getMetric(category: string, metricName: string): MetricData | undefined {
        const summary = this.summary();
        if (!summary?.metrics) {
            return undefined;
        }
        return summary.metrics[category]?.[metricName];
    }

    /**
     * Gets all metrics for a category.
     */
    getCategoryMetrics(category: string): Record<string, MetricData> | undefined {
        const summary = this.summary();
        return summary?.metrics?.[category];
    }

    /**
     * Gets all available categories.
     */
    getCategories(): string[] {
        const summary = this.summary();
        if (!summary?.metrics) {
            return [];
        }
        return Object.keys(summary.metrics);
    }

    /**
     * Gets the i18n key for a category's display title.
     * Format: usage.category.{category}.title
     *
     * @param category the category name (e.g., "content")
     * @return the i18n key (e.g., "usage.category.content.title")
     */
    getCategoryTitleKey(category: string): string {
        return `usage.category.${category}.title`;
    }

    /**
     * Checks if a string is an i18n key (starts with a known prefix).
     */
    isI18nKey(value: string): boolean {
        return value.startsWith('usage.dashboard.');
    }
}
