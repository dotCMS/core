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

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPageScanLoadingComponent } from './dot-page-scan-loading/dot-page-scan-loading.component';
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
    isPrivateUrlError: boolean;
    tunnelConsentAccepted: boolean;
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
        DotMessagePipe,
        DotPageScannerA11yReportComponent,
        DotPageScannerGeoReportComponent,
        DotPageScanLoadingComponent
    ],
    templateUrl: './dot-page-scanner-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerReportComponent implements OnDestroy {
    protected readonly $state = signalState<DotPageScannerState>({
        visible: false,
        status: 'idle',
        error: null,
        isPrivateUrlError: false,
        tunnelConsentAccepted: false,
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
        const isPrivateUrlError = this.isPrivateUrl(url);

        patchState(this.$state, {
            reportType: type,
            pageUrl: url,
            visible: true,
            status: isPrivateUrlError ? 'done' : 'pending',
            error: null,
            isPrivateUrlError,
            a11yData: null,
            geoData: null
        });

        if (!isPrivateUrlError) {
            this.runScan();
        }
    }

    private isPrivateUrl(url: string): boolean {
        try {
            const { hostname } = new URL(url);
            return (
                hostname === 'localhost' ||
                hostname === '127.0.0.1' ||
                hostname === '::1' ||
                hostname.endsWith('.local') ||
                /^10\./.test(hostname) ||
                /^192\.168\./.test(hostname) ||
                /^172\.(1[6-9]|2\d|3[01])\./.test(hostname)
            );
        } catch {
            return false;
        }
    }

    acceptTunnelConsent(): void {
        patchState(this.$state, { tunnelConsentAccepted: true });
    }

    onDialogHide(): void {
        this.cancel$.next();
        patchState(this.$state, { status: 'idle', tunnelConsentAccepted: false });
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
                        const errorBody = err?.error;
                        const isPrivateUrlError =
                            errorBody?.ok === false &&
                            typeof errorBody?.error === 'string' &&
                            errorBody.error.toLowerCase().includes('private');
                        patchState(this.$state, {
                            error:
                                errorBody?.error ??
                                err?.message ??
                                'An error occurred while scanning the page.',
                            isPrivateUrlError,
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
                        const errorBody = err?.error;
                        const isPrivateUrlError =
                            errorBody?.ok === false &&
                            typeof errorBody?.error === 'string' &&
                            errorBody.error.toLowerCase().includes('private');
                        patchState(this.$state, {
                            error:
                                errorBody?.error ??
                                err?.message ??
                                'An error occurred while scanning the page.',
                            isPrivateUrlError,
                            status: 'done'
                        });
                        this.cdr.markForCheck();
                    }
                });
        }
    }
}
