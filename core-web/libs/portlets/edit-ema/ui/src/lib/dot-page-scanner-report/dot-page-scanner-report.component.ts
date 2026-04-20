import { patchState, signalState } from '@ngrx/signals';
import { Observable, Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPageScanLoadingComponent } from './dot-page-scan-loading/dot-page-scan-loading.component';
import { DotPageScannerA11yReportComponent } from './dot-page-scanner-a11y-report/dot-page-scanner-a11y-report.component';
import { DotPageScannerGeoReportComponent } from './dot-page-scanner-geo-report/dot-page-scanner-geo-report.component';
import { DotPageScannerMessageComponent } from './dot-page-scanner-message/dot-page-scanner-message.component';
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
    isNotConfiguredError: boolean;
    isAuthFailedError: boolean;
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
        DotPageScanLoadingComponent,
        DotPageScannerMessageComponent
    ],
    templateUrl: './dot-page-scanner-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerReportComponent {
    protected readonly $state = signalState<DotPageScannerState>({
        visible: false,
        status: 'idle',
        error: null,
        isPrivateUrlError: false,
        isNotConfiguredError: false,
        isAuthFailedError: false,
        tunnelConsentAccepted: false,
        reportType: 'a11y',
        pageUrl: '',
        a11yData: null,
        geoData: null
    });

    private readonly scanner = inject(DotPageScannerService);
    private readonly cancel$ = new Subject<void>();
    // Created in injection context so takeUntilDestroyed works correctly
    private readonly takeUntilDestroyed = takeUntilDestroyed();

    open(type: ReportType, url: string): void {
        patchState(this.$state, {
            reportType: type,
            pageUrl: url,
            visible: true,
            status: 'pending',
            error: null,
            isPrivateUrlError: false,
            isNotConfiguredError: false,
            isAuthFailedError: false,
            a11yData: null,
            geoData: null
        });

        this.runScan();
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

    private runScan(): void {
        const url = this.$state.pageUrl();
        const type = this.$state.reportType();
        const scan$: Observable<PageScannerA11yResponse | PageScannerGeoResponse> =
            type === 'a11y' ? this.scanner.checkA11y(url) : this.scanner.checkGeo(url);

        scan$.pipe(takeUntil(this.cancel$), this.takeUntilDestroyed).subscribe({
            next: (data) => {
                const update =
                    type === 'a11y'
                        ? { a11yData: data as PageScannerA11yResponse }
                        : { geoData: data as PageScannerGeoResponse };
                patchState(this.$state, { ...update, status: 'done' });
            },
            error: (err) => {
                patchState(this.$state, { ...this.parseScanError(err), status: 'done' });
            }
        });
    }

    private parseScanError(err: unknown): {
        error: string;
        isPrivateUrlError: boolean;
        isNotConfiguredError: boolean;
        isAuthFailedError: boolean;
    } {
        const httpErr = err as {
            error?: { entity?: { errorCode?: string }; ok?: boolean; error?: string };
        };
        const errorCode = httpErr?.error?.entity?.errorCode;
        const isNotConfiguredError = errorCode === 'PAGE_SCANNER_NOT_CONFIGURED';
        const isAuthFailedError = errorCode === 'PAGE_SCANNER_AUTH_FAILED';

        const errorBody = httpErr?.error;
        const isPrivateUrlError =
            errorBody?.ok === false &&
            typeof errorBody?.error === 'string' &&
            errorBody.error.toLowerCase().includes('private');

        return {
            error:
                errorBody?.error ??
                (err as { message?: string })?.message ??
                'An error occurred while scanning the page.',
            isPrivateUrlError,
            isNotConfiguredError,
            isAuthFailedError
        };
    }
}
