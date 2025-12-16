import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    ContentConversionRow,
    DotAnalyticsDashboardStore,
    MetricData,
    transformContentConversionsData,
    transformConversionTrendData,
    transformTrafficVsConversionsData
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { TIME_PERIOD_OPTIONS } from '../../constants';
import { ChartData } from '../../types';
import { DotAnalyticsDashboardChartComponent } from '../dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardMetricsComponent } from '../dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Conversions Report Component
 *
 * Displays conversion analytics including metrics, charts, and content conversions table.
 * Uses lazy loading - data is loaded when the component initializes (when tab is selected).
 *
 * TODO: Implement date range validation for conversions dashboard.
 * The date range filter should not allow more than 1 month between start and end dates.
 * This limitation is specific to the conversions section due to data volume constraints.
 */
@Component({
    selector: 'dot-analytics-dashboard-conversions-report',
    imports: [
        CommonModule,
        CardModule,
        TableModule,
        TagModule,
        DotAnalyticsDashboardMetricsComponent,
        DotAnalyticsDashboardChartComponent,
        DotAnalyticsStateMessageComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard-conversions-report.component.html',
    styleUrl: './dot-analytics-dashboard-conversions-report.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsDashboardConversionsReportComponent implements OnInit {
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #globalStore = inject(GlobalStore);
    readonly #messageService = inject(DotMessageService);

    ngOnInit(): void {
        // Lazy load conversions data when component initializes (tab selected)
        this.store.loadConversionsData();
    }

    // Dynamic chart title with time range from filter
    protected readonly $trafficVsConversionsTitle = computed(() => {
        const timeRange = this.store.timeRange();
        const baseTitle = this.#messageService.get('analytics.charts.traffic-vs-conversions.title');

        // Handle custom date range (array of [from, to])
        if (Array.isArray(timeRange)) {
            const [from, to] = timeRange;

            return `${baseTitle} (${from} - ${to})`;
        }

        // Handle predefined time range options
        const option = TIME_PERIOD_OPTIONS.find((opt) => opt.value === timeRange);
        const timeRangeLabel = option ? this.#messageService.get(option.label) : '';

        return timeRangeLabel ? `${baseTitle} (${timeRangeLabel})` : baseTitle;
    });

    // Content conversions table data from ContentAttribution cube
    protected readonly $contentConversionsData = computed<ContentConversionRow[]>(() => {
        const contentConversions = this.store.contentConversions();

        return transformContentConversionsData(contentConversions.data);
    });
    protected readonly $contentConversionsStatus = computed(
        () => this.store.contentConversions().status
    );

    // Table state helpers
    protected readonly $isTableLoading = computed(() => {
        const status = this.$contentConversionsStatus();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isTableError = computed(
        () => this.$contentConversionsStatus() === ComponentStatus.ERROR
    );

    protected readonly $isTableEmpty = computed(() => {
        const data = this.$contentConversionsData();
        const status = this.$contentConversionsStatus();

        return status === ComponentStatus.LOADED && (!data || data.length === 0);
    });

    // Computed signals for conversions metrics
    protected readonly $metricsData = computed((): MetricData[] => {
        const totalConversions = this.store.totalConversions();
        const convertingVisitors = this.store.convertingVisitors();

        const totalConversionsValue = totalConversions.data
            ? parseInt(totalConversions.data['EventSummary.totalEvents'], 10)
            : 0;

        const uniqueVisitors = convertingVisitors.data
            ? parseInt(convertingVisitors.data['EventSummary.uniqueVisitors'], 10)
            : 0;

        const uniqueConvertingVisitors = convertingVisitors.data
            ? parseInt(convertingVisitors.data['EventSummary.uniqueConvertingVisitors'], 10)
            : 0;

        // Site Conversion Rate = (uniqueConvertingVisitors / uniqueVisitors) * 100
        const conversionRate =
            uniqueVisitors > 0
                ? Math.round((uniqueConvertingVisitors / uniqueVisitors) * 10000) / 100
                : 0;

        return [
            {
                name: 'analytics.metrics.total-conversions',
                value: totalConversionsValue,
                subtitle: 'analytics.metrics.total-conversions.subtitle',
                icon: 'pi-check-circle',
                status: totalConversions.status,
                error: totalConversions.error
            },
            {
                name: 'analytics.metrics.converting-visitors',
                value: uniqueConvertingVisitors,
                subtitle: 'analytics.metrics.converting-visitors.subtitle',
                icon: 'pi-users',
                status: convertingVisitors.status,
                error: convertingVisitors.error
            },
            {
                name: 'analytics.metrics.site-conversion-rate',
                value: conversionRate,
                subtitle: 'analytics.metrics.site-conversion-rate.subtitle',
                icon: 'pi-chart-line',
                status: convertingVisitors.status,
                error: convertingVisitors.error
            }
        ];
    });

    // Chart 1: Conversion Trend - simple line chart (1 dataset)
    protected readonly $conversionTrendData = computed<ChartData>(() => {
        const conversionTrend = this.store.conversionTrend();

        return transformConversionTrendData(conversionTrend.data);
    });
    protected readonly $conversionTrendStatus = computed(() => this.store.conversionTrend().status);

    // Chart 2: Traffic vs Conversions - combo chart (2 datasets: bar + line)
    // Bars: uniqueVisitors per day, Line: conversion rate % per day
    protected readonly $trafficVsConversionsData = computed<ChartData>(() => {
        const trafficVsConversions = this.store.trafficVsConversions();

        return transformTrafficVsConversionsData(trafficVsConversions.data);
    });
    protected readonly $trafficVsConversionsStatus = computed(
        () => this.store.trafficVsConversions().status
    );
}
