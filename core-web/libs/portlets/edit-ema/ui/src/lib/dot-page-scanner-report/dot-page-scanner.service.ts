import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface PageScannerA11yItem {
    code: string;
    type: 'error' | 'warning' | 'notice';
    typeCode: number;
    message: string;
    context: string;
    selector: string;
    runner: string;
    runnerExtras: {
        description: string;
        impact: string;
        help: string;
        helpUrl: string;
    };
}

export interface PageScannerA11yResponse {
    ok: boolean;
    checkId: string;
    checkedAt: string;
    pageUrl: string;
    documentTitle: string;
    standard: string;
    runners: string[];
    authenticatedRequest: boolean;
    authHeaderMode: string;
    counts: {
        errors: number;
        warnings: number;
        notices: number;
    };
    totalIssues: number;
    findings: {
        total: number;
        byType: {
            errors: number;
            warnings: number;
            notices: number;
        };
        items: PageScannerA11yItem[];
    };
    issues: PageScannerA11yItem[];
    screenshot: {
        captured: boolean;
        fileName: string;
        endpoint: string;
        mimeType: string;
    };
}

export interface PageScannerGeoSignal {
    score: number;
    value: unknown;
    message: string;
}

export interface PageScannerGeoCategory {
    score: number;
    weight: number;
    signals: {
        [signalKey: string]: PageScannerGeoSignal;
    };
}

export interface PageScannerGeoIssue {
    severity: 'high' | 'medium' | 'low';
    signal: string;
    message: string;
}

export interface PageScannerGeoResponse {
    url: string;
    score: number;
    fetchedAt: string;
    categories: {
        [categoryKey: string]: PageScannerGeoCategory;
    };
    topIssues: PageScannerGeoIssue[];
}

@Injectable()
export class DotPageScannerService {
    private http = inject(HttpClient);

    checkA11y(url: string): Observable<PageScannerA11yResponse> {
        return this.http.post<PageScannerA11yResponse>('/api/v1/page-scanner/a11y/check', { url });
    }

    checkGeo(url: string): Observable<PageScannerGeoResponse> {
        return this.http.post<PageScannerGeoResponse>('/api/v1/page-scanner/geo/check', { url });
    }
}
