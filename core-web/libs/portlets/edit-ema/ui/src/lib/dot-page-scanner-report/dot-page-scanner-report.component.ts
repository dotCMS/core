import { Subject } from 'rxjs';

import { DecimalPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnDestroy,
    signal
} from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';
import { SkeletonModule } from 'primeng/skeleton';

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import {
    DotPageScannerService,
    PageScannerA11yItem,
    PageScannerA11yResponse,
    PageScannerGeoIssue,
    PageScannerGeoResponse
} from './dot-page-scanner.service';

export type ReportType = 'a11y' | 'geo';

interface A11yGroup {
    code: string;
    type: 'error' | 'warning' | 'notice';
    impact: string;
    helpUrl: string;
    items: PageScannerA11yItem[];
    count: number;
}

interface GeoCategory {
    key: string;
    label: string;
    score: number;
    weight: number;
    passedCount: number;
    totalCount: number;
    signals: Array<{ key: string; score: number; message: string }>;
}

/** CSS custom property overrides for p-chip color variants */
const CHIP_STYLES = {
    red: {
        '--p-chip-background': 'var(--p-red-100)',
        '--p-chip-color': 'var(--p-red-700)'
    },
    yellow: {
        '--p-chip-background': 'var(--p-yellow-100)',
        '--p-chip-color': 'var(--p-yellow-700)'
    },
    blue: {
        '--p-chip-background': 'var(--p-blue-100)',
        '--p-chip-color': 'var(--p-blue-700)'
    },
    green: {
        '--p-chip-background': 'var(--p-green-100)',
        '--p-chip-color': 'var(--p-green-800)'
    }
} as const;

@Component({
    selector: 'dot-page-scanner-report',
    standalone: true,
    providers: [DotPageScannerService],
    imports: [
        DecimalPipe,
        DialogModule,
        AccordionModule,
        BadgeModule,
        ButtonModule,
        CardModule,
        ChartModule,
        ChipModule,
        SkeletonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-page-scanner-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerReportComponent implements OnDestroy {
    protected visible = signal(false);
    protected loading = signal(false);
    protected error = signal<string | null>(null);
    protected reportType = signal<ReportType>('a11y');
    protected pageUrl = signal<string>('');
    protected a11yData = signal<PageScannerA11yResponse | null>(null);
    protected geoData = signal<PageScannerGeoResponse | null>(null);
    protected a11yGroups = signal<A11yGroup[]>([]);
    protected geoCategories = signal<GeoCategory[]>([]);
    protected geoChartData = signal<object | null>(null);

    private readonly scanner = inject(DotPageScannerService);
    private readonly cdr = inject(ChangeDetectorRef);
    private cancel$ = new Subject<void>();

    /**
     * Open the scanner report dialog for the given type and page URL.
     */
    open(type: ReportType, url: string): void {
        this.reportType.set(type);
        this.pageUrl.set(url);
        this.visible.set(true);
        this.error.set(null);
        this.a11yData.set(null);
        this.geoData.set(null);
        this.loading.set(true);
        this.runScan();
    }

    onDialogHide(): void {
        this.cancel$.next();
        this.loading.set(false);
    }

    getScoreLabel(score: number): string {
        if (score >= 80) return 'Excellent';
        if (score >= 50) return 'Good';
        return 'Needs Work';
    }

    /**
     * Returns CSS custom property overrides for a score-based chip.
     * score 80-100 → green, 50-79 → yellow, 0-49 → red
     */
    getScoreChipStyle(score: number): Record<string, string> {
        if (score >= 80) return CHIP_STYLES.green;
        if (score >= 50) return CHIP_STYLES.yellow;
        return CHIP_STYLES.red;
    }

    /**
     * Returns CSS custom property overrides for an impact-severity chip.
     * critical / serious → red, moderate → yellow, minor / _ → blue
     */
    getImpactChipStyle(impact: string): Record<string, string> {
        if (impact === 'critical' || impact === 'serious') return CHIP_STYLES.red;
        if (impact === 'moderate') return CHIP_STYLES.yellow;
        return CHIP_STYLES.blue;
    }

    /**
     * Returns CSS custom property overrides for a finding-type chip.
     * error → red, warning → yellow, notice → blue
     */
    getTypeChipStyle(type: string): Record<string, string> {
        if (type === 'error') return CHIP_STYLES.red;
        if (type === 'warning') return CHIP_STYLES.yellow;
        return CHIP_STYLES.blue;
    }

    /**
     * Returns CSS custom property overrides for a geo severity chip.
     * high → red, medium → yellow, low → blue
     */
    getSeverityChipStyle(severity: string): Record<string, string> {
        if (severity === 'high') return CHIP_STYLES.red;
        if (severity === 'medium') return CHIP_STYLES.yellow;
        return CHIP_STYLES.blue;
    }

    camelToTitle(key: string): string {
        return key.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase());
    }

    countBySeverity(issues: PageScannerGeoIssue[], severity: string): number {
        return issues.filter((i) => i.severity === severity).length;
    }

    ngOnDestroy(): void {
        this.cancel$.next();
        this.cancel$.complete();
    }

    private runScan(): void {
        const url = this.pageUrl();
        const type = this.reportType();

        if (type === 'a11y') {
            this.scanner
                .checkA11y(url)
                .pipe(takeUntil(this.cancel$))
                .subscribe({
                    next: (data) => {
                        this.a11yData.set(data);
                        this.a11yGroups.set(this.buildA11yGroups(data));
                        this.loading.set(false);
                        this.cdr.markForCheck();
                    },
                    error: (err) => {
                        this.error.set(
                            err?.message ?? 'An error occurred while scanning the page.'
                        );
                        this.loading.set(false);
                        this.cdr.markForCheck();
                    }
                });
        } else {
            this.scanner
                .checkGeo(url)
                .pipe(takeUntil(this.cancel$))
                .subscribe({
                    next: (data) => {
                        this.geoData.set(data);
                        this.geoCategories.set(this.buildGeoCategories(data));
                        this.geoChartData.set(this.buildGeoChartData(data.score));
                        this.loading.set(false);
                        this.cdr.markForCheck();
                    },
                    error: (err) => {
                        this.error.set(
                            err?.message ?? 'An error occurred while scanning the page.'
                        );
                        this.loading.set(false);
                        this.cdr.markForCheck();
                    }
                });
        }
    }

    private buildA11yGroups(data: PageScannerA11yResponse): A11yGroup[] {
        const items = data.findings?.items ?? data.issues ?? [];
        const map = new Map<string, A11yGroup>();

        for (const item of items) {
            if (map.has(item.code)) {
                map.get(item.code)!.items.push(item);
                map.get(item.code)!.count++;
            } else {
                map.set(item.code, {
                    code: item.code,
                    type: item.type,
                    impact: item.runnerExtras?.impact ?? '',
                    helpUrl: item.runnerExtras?.helpUrl ?? '',
                    items: [item],
                    count: 1
                });
            }
        }

        return Array.from(map.values());
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
