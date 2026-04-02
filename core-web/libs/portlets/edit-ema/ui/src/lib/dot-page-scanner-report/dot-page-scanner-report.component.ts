import { patchState, signalState } from '@ngrx/signals';
import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnDestroy
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

type ScanStatus = 'idle' | 'pending' | 'done';

interface DotPageScannerState {
    visible: boolean;
    status: ScanStatus;
    error: string | null;
    reportType: ReportType;
    pageUrl: string;
    a11yData: PageScannerA11yResponse | null;
    geoData: PageScannerGeoResponse | null;
}

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
    protected readonly $state = signalState<DotPageScannerState>({
        visible: false,
        status: 'idle',
        error: null,
        reportType: 'a11y',
        pageUrl: '',
        a11yData: null,
        geoData: null
    });

    private readonly scanner = inject(DotPageScannerService);
    private readonly cdr = inject(ChangeDetectorRef);
    private cancel$ = new Subject<void>();

    /**
     * Open the scanner report dialog for the given type and page URL.
     */
    open(type: ReportType, url: string): void {
        patchState(this.$state, {
            reportType: type,
            pageUrl: url,
            visible: true,
            status: 'pending',
            error: null,
            a11yData: null,
            geoData: null
        });

        this.runScan();
    }

    onDialogHide(): void {
        this.cancel$.next();
        patchState(this.$state, { status: 'idle' });
    }

    onVisibleChange(value: boolean): void {
        patchState(this.$state, { visible: value });
    }

    ngOnDestroy(): void {
        this.cancel$.next();
        this.cancel$.complete();
    }

    private runScan(): void {
        const url = this.$state.pageUrl();
        const type = this.$state.reportType();

        if (type === 'a11y') {
            this.scanner
                .checkA11y(url)
                .pipe(takeUntil(this.cancel$))
                .subscribe({
                    next: (data) => {
                        patchState(this.$state, { a11yData: data, status: 'done' });
                        this.cdr.markForCheck();
                    },
                    error: (err) => {
                        patchState(this.$state, {
                            error: err?.message ?? 'An error occurred while scanning the page.',
                            status: 'done'
                        });
                        this.cdr.markForCheck();
                    }
                });
        } else {
            this.scanner
                .checkGeo(url)
                .pipe(takeUntil(this.cancel$))
                .subscribe({
                    next: (data) => {
                        patchState(this.$state, { geoData: data, status: 'done' });
                        this.cdr.markForCheck();
                    },
                    error: (err) => {
                        patchState(this.$state, {
                            error: err?.message ?? 'An error occurred while scanning the page.',
                            status: 'done'
                        });
                        this.cdr.markForCheck();
                    }
                });
        }
    }
}
