import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, computed, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessagesModule } from 'primeng/messages';
import { SkeletonModule } from 'primeng/skeleton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipe } from '@dotcms/ui';

import { DotUsageService, MetricData } from '../services/dot-usage.service';

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
export class DotUsageShellComponent implements OnInit {
    private readonly usageService = inject(DotUsageService);

    // Reactive state from service
    readonly summary = this.usageService.summary;
    readonly loading = this.usageService.loading;
    readonly error = this.usageService.error;
    readonly errorStatus = this.usageService.errorStatus;

    // Computed values for display
    readonly hasData = computed(() => this.summary() !== null);
    readonly lastUpdated = signal<Date | null>(null);
    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.usageService.getSummary().subscribe({
            next: () => {
                // Data is automatically updated via signals
                this.lastUpdated.set(new Date());
            },
            error: (error) => {
                console.error('Failed to load usage data:', error);
            }
        });
    }

    onRefresh(): void {
        this.loadData();
    }

    onRetry(): void {
        this.usageService.reset();
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
