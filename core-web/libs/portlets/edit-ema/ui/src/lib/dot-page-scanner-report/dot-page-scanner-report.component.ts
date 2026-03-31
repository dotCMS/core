import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnDestroy,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SkeletonModule } from 'primeng/skeleton';

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPageScannerA11yReportComponent } from './dot-page-scanner-a11y-report/dot-page-scanner-a11y-report.component';
import { DotPageScannerGeoReportComponent } from './dot-page-scanner-geo-report/dot-page-scanner-geo-report.component';
import {
    DotPageScannerService,
    PageScannerA11yResponse,
    PageScannerGeoResponse
} from './dot-page-scanner.service';
import { ReportType } from './models';

@Component({
    selector: 'dot-page-scanner-report',
    standalone: true,
    providers: [DotPageScannerService],
    imports: [
        DialogModule,
        ButtonModule,
        SkeletonModule,
        DotMessagePipe,
        DotPageScannerA11yReportComponent,
        DotPageScannerGeoReportComponent
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
}
