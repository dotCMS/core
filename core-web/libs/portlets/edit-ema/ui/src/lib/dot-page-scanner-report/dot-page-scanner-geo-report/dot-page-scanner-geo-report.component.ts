import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { ChipModule } from 'primeng/chip';

import { DotMessagePipe } from '@dotcms/ui';

import { CHIP_STYLES } from '../chip-styles';
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

    protected geoCategories = computed(() => this.buildGeoCategories(this.geoData()));
    protected geoChartData = computed(() => this.buildGeoChartData(this.geoData().score));

    protected scoreLabel = computed(() => {
        const score = this.geoData().score;
        if (score >= 80) return 'Excellent';
        if (score >= 50) return 'Good';
        return 'Needs Work';
    });

    protected scoreLabelClass = computed(() => {
        const score = this.geoData().score;
        if (score >= 80) return 'text-green-500';
        if (score >= 50) return 'text-yellow-500';
        return 'text-red-500';
    });

    protected scoreChipStyle = computed(() => this.getScoreChipStyle(this.geoData().score));

    protected severityCounts = computed<Record<string, number>>(() => {
        const issues = this.geoData().topIssues ?? [];
        return {
            high: issues.filter((i) => i.severity === 'high').length,
            medium: issues.filter((i) => i.severity === 'medium').length,
            low: issues.filter((i) => i.severity === 'low').length
        };
    });

    protected topIssuesWithStyle = computed(() =>
        (this.geoData().topIssues ?? []).map((issue) => ({
            ...issue,
            chipStyle: this.getSeverityChipStyle(issue.severity)
        }))
    );

    camelToTitle(key: string): string {
        return key.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase());
    }

    private getScoreChipStyle(score: number): Record<string, string> {
        if (score >= 80) return CHIP_STYLES.green;
        if (score >= 50) return CHIP_STYLES.yellow;
        return CHIP_STYLES.red;
    }

    private getScoreBadgeClass(score: number): string {
        if (score >= 80) return 'bg-green-100 text-green-800 border-green-200';
        if (score >= 50) return 'bg-yellow-100 text-yellow-700 border-yellow-200';
        return 'bg-red-100 text-red-700 border-red-200';
    }

    private getSeverityChipStyle(severity: string): Record<string, string> {
        if (severity === 'high') return CHIP_STYLES.red;
        if (severity === 'medium') return CHIP_STYLES.yellow;
        return CHIP_STYLES.blue;
    }

    private buildGeoCategories(data: PageScannerGeoResponse): GeoCategory[] {
        return Object.entries(data.categories ?? {}).map(([key, cat]) => {
            const signals = Object.entries(cat.signals ?? {}).map(([sKey, sig]) => ({
                key: sKey,
                score: sig.score,
                message: sig.message,
                chipStyle: this.getScoreChipStyle(sig.score)
            }));
            const passed = signals.filter((s) => s.score >= 80).length;

            return {
                key,
                label: this.camelToTitle(key),
                score: cat.score,
                weight: cat.weight,
                passedCount: passed,
                totalCount: signals.length,
                chipStyle: this.getScoreChipStyle(cat.score),
                badgeClass: this.getScoreBadgeClass(cat.score),
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
