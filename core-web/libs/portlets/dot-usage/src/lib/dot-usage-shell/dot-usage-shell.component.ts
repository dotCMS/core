import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, computed } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessagesModule } from 'primeng/messages';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { DotUsageService, MetricData } from '../services/dot-usage.service';

/**
 * Mapping of metric names to icons.
 * Display labels come from the API response (MetricType.getDisplayLabel()).
 */
const METRIC_ICONS: Record<string, string> = {
    // Site metrics
    COUNT_OF_SITES: 'pi-globe',
    COUNT_OF_ACTIVE_SITES: 'pi-check-circle',
    COUNT_OF_TEMPLATES: 'pi-clone',
    ALIASES_SITES_COUNT: 'pi-link',

    // Content metrics
    COUNT: 'pi-file',
    COUNT_CONTENT: 'pi-file',
    CONTENT_TYPES_ASSIGNED: 'pi-sitemap',
    CONTENTS_RECENTLY_EDITED: 'pi-pencil',
    LAST_CONTENT_EDITED: 'pi-clock',

    // User metrics
    ACTIVE_USERS_COUNT: 'pi-users',
    COUNT_OF_USERS: 'pi-user',
    LAST_LOGIN: 'pi-calendar',

    // System metrics
    COUNT_LANGUAGES: 'pi-flag',
    SCHEMES_COUNT: 'pi-share-alt',
    STEPS_COUNT: 'pi-sitemap',
    COUNT_OF_LIVE_CONTAINERS: 'pi-box',
    COUNT_OF_TEMPLATE_BUILDER_TEMPLATES: 'pi-th-large'
};

/**
 * Mapping of category names to display titles.
 */
const CATEGORY_TITLES: Record<string, string> = {
    site: 'Site Metrics',
    content: 'Content Metrics',
    user: 'User Activity',
    system: 'System Configuration'
};

@Component({
    selector: 'lib-dot-usage-shell',
    imports: [
        CommonModule,
        ButtonModule,
        CardModule,
        MessagesModule,
        SkeletonModule,
        TooltipModule
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

    // Computed values for display
    readonly hasData = computed(() => this.summary() !== null);
    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.usageService.getSummary().subscribe({
            next: () => {
                // Data is automatically updated via signals
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
            return 'N/A';
        }

        // If it's already a string, check if it's a number string
        if (typeof value === 'string') {
            const numValue = parseFloat(value);
            if (!isNaN(numValue)) {
                // It's a numeric string, format it as a number
                return this.formatNumber(numValue);
            }
            // It's a non-numeric string, return as-is
            return value || 'N/A';
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
     * Gets the icon class for a metric name.
     */
    getMetricIcon(metricName: string): string {
        return METRIC_ICONS[metricName] || 'pi-circle';
    }

    /**
     * Gets the display title for a category.
     */
    getCategoryTitle(category: string): string {
        return CATEGORY_TITLES[category] || category.charAt(0).toUpperCase() + category.slice(1);
    }
}
