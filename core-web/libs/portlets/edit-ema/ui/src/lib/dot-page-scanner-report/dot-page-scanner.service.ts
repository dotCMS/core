import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

/**
 * Severity reported by axe-core for a rule/node.
 */
export type AxeImpact = 'critical' | 'serious' | 'moderate' | 'minor' | null;

/**
 * A single DOM element flagged by an axe rule.
 */
export interface AxeNode {
    html: string;
    target: string[];
    impact: AxeImpact;
    failureSummary: string;
}

/**
 * A raw axe-core rule result. The same shape is used for both `violations`
 * (confirmed failures) and `incomplete` (needs manual review).
 */
export interface AxeRule {
    id: string;
    impact: AxeImpact;
    tags: string[];
    description: string;
    help: string;
    helpUrl: string;
    nodes: AxeNode[];
}

/**
 * Raw axe-core run payload as returned by the external scanner.
 */
export interface AxeResult {
    testEngine: { name: string; version: string };
    testRunner: { name: string };
    timestamp: string;
    url: string;
    violations: AxeRule[];
    incomplete: AxeRule[];
}

export interface PageScannerA11yResponse {
    ok: boolean;
    checkId: string;
    checkedAt: string;
    pageUrl: string;
    documentTitle: string;
    standard: string;
    runners: string[];
    stylesheets: string[];
    authenticatedRequest: boolean;
    authHeaderMode: string;
    axe: AxeResult;
    screenshot: {
        captured: boolean;
        fileName?: string;
        endpoint?: string;
        mimeType?: string;
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
