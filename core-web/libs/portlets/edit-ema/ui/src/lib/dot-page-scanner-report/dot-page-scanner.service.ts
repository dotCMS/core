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
        // Never scan in EDIT_MODE: dotCMS injects editor-only chrome (drag handles,
        // add-content buttons, etc.) into the EDIT_MODE render, which axe flags as
        // accessibility violations that don't exist on the real page. Force
        // PREVIEW_MODE so the scan sees the page as visitors do. Enforced here at the
        // single chokepoint so no caller can accidentally scan EDIT_MODE.
        const scanUrl = this.forcePreviewMode(url);

        // ============================================================================
        // ⚠️⚠️⚠️ DEV-ONLY HACK — REMOVE BEFORE PRODUCTION ⚠️⚠️⚠️
        // ----------------------------------------------------------------------------
        // The scanner runs on the dotCMS backend and renders the URL server-side, so
        // it cannot reach the Angular dev server on :4200. We rewrite :4200 → :8080
        // so the scan hits the backend in local dev.
        //
        // This is WRONG for production: it hardcodes ports into a shared library used
        // by UVE, and prod has no :4200/:8080 split. MUST be reverted — build the
        // reachable scan URL in the caller (env-aware) instead.
        // ============================================================================
        return this.http.post<PageScannerA11yResponse>('/api/v1/page-scanner/a11y/check', {
            url: scanUrl.replace('4200', '8080')
        });
    }

    /** Rewrite any `mode=EDIT_MODE` on the URL to `PREVIEW_MODE` (see checkA11y). */
    private forcePreviewMode(url: string): string {
        try {
            const parsed = new URL(url, window.location.origin);
            if (parsed.searchParams.get('mode') === 'EDIT_MODE') {
                parsed.searchParams.set('mode', 'PREVIEW_MODE');
            }
            return parsed.toString();
        } catch {
            // Fall back to a plain string replace for non-absolute / unparseable URLs.
            return url.replace('mode=EDIT_MODE', 'mode=PREVIEW_MODE');
        }
    }

    checkGeo(url: string): Observable<PageScannerGeoResponse> {
        return this.http.post<PageScannerGeoResponse>('/api/v1/page-scanner/geo/check', { url });
    }
}
