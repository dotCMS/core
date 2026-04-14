import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { ChipModule } from 'primeng/chip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { PageScannerGeoResponse } from '../dot-page-scanner.service';
import { GeoCategory } from '../models';

@Component({
    selector: 'dot-page-scanner-geo-report',
    standalone: true,
    imports: [AccordionModule, CardModule, ChartModule, ChipModule, DecimalPipe, DotMessagePipe],
    templateUrl: './dot-page-scanner-geo-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerGeoReportComponent {
    geoData = input.required<PageScannerGeoResponse>();

    private readonly dotMessage = inject(DotMessageService);

    protected geoCategories = computed(() => this.buildGeoCategories(this.geoData()));
    protected geoChartData = computed(() => this.buildGeoChartData(this.geoData().score));

    protected scoreLabel = computed(() => {
        const score = this.geoData().score;
        if (score >= 80) return this.dotMessage.get('page.scanner.geo.score.label.excellent');
        if (score >= 50) return this.dotMessage.get('page.scanner.geo.score.label.good');
        return this.dotMessage.get('page.scanner.geo.score.label.needs.work');
    });

    protected scoreLabelClass = computed(() => {
        const score = this.geoData().score;
        if (score >= 80) return 'text-green-500';
        if (score >= 50) return 'text-yellow-500';
        return 'text-red-500';
    });

    protected severityCounts = computed<Record<string, number>>(() => {
        const issues = this.geoData().topIssues ?? [];
        return {
            high: issues.filter((i) => i.severity === 'high').length,
            medium: issues.filter((i) => i.severity === 'medium').length,
            low: issues.filter((i) => i.severity === 'low').length
        };
    });

    camelToTitle(key: string): string {
        return key.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase());
    }

    private buildGeoCategories(data: PageScannerGeoResponse): GeoCategory[] {
        return Object.entries(data.categories ?? {}).map(([key, cat]) => {
            const signals = Object.entries(cat.signals ?? {}).map(([sKey, sig]) => ({
                key: sKey,
                score: sig.score,
                message: sig.message
            }));
            const passed = signals.filter((s) => s.score >= 80).length;

            return {
                key,
                label: this.camelToTitle(key),
                score: cat.score,
                weight: cat.weight,
                passedCount: passed,
                totalCount: signals.length,
                signals
            };
        });
    }

    private buildGeoChartData(score: number): object {
        return {
            datasets: [
                {
                    data: [score, 100 - score],
                    backgroundColor: [this.scoreColor(score), '#e5e7eb'],
                    borderWidth: 0
                }
            ]
        };
    }

    private scoreColor(score: number): string {
        if (score >= 80) return '#22c55e';
        if (score >= 50) return '#eab308';
        return '#ef4444';
    }
}
