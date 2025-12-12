import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';

import { catchError, map, tap } from 'rxjs/operators';

/**
 * Metric metadata structure containing name, value, and display label.
 */
export interface MetricData {
    readonly name: string;
    readonly value: number | string;
    readonly displayLabel: string;
}

/**
 * Dynamic usage summary that adapts to available metrics based on profile.
 *
 * Metrics are organized by category (e.g., "content", "site", "user", "system")
 * as defined by their @DashboardMetric annotation. Each category contains a map
 * of metric names to their metadata (name, value, displayLabel). Only metrics
 * available for the active profile (MINIMAL, STANDARD, FULL) are included.
 */
export interface UsageSummary {
    /**
     * Metrics organized by category. Each category contains a map of
     * metric names to their metadata. Only metrics available for the
     * active profile are included.
     *
     * Example structure:
     * {
     *   "content": {
     *     "COUNT_CONTENT": {
     *       "name": "COUNT_CONTENT",
     *       "value": 12345,
     *       "displayLabel": "Total Content"
     *     }
     *   },
     *   "site": {
     *     "COUNT_OF_SITES": {
     *       "name": "COUNT_OF_SITES",
     *       "value": 10,
     *       "displayLabel": "Total Sites"
     *     }
     *   }
     * }
     */
    readonly metrics: Record<string, Record<string, MetricData>>;
    readonly lastUpdated: string;
}

/**
 * API response wrapper for usage summary
 */
export interface UsageApiResponse {
    readonly entity: UsageSummary;
}

/**
 * HTTP error response structure
 */
export interface UsageErrorResponse {
    readonly error?: {
        readonly message?: string;
    };
    readonly status?: number;
    readonly statusText?: string;
}

/**
 * Service state interface for reactive state management
 */
export interface UsageServiceState {
    readonly summary: UsageSummary | null;
    readonly loading: boolean;
    readonly error: string | null;
}

@Injectable({
    providedIn: 'root'
})
export class DotUsageService {
    #BASE_URL = '/api/v1/usage';
    #http = inject(HttpClient);

    // Reactive state
    readonly summary = signal<UsageSummary | null>(null);
    readonly loading = signal<boolean>(false);
    readonly error = signal<string | null>(null);
    readonly errorStatus = signal<number | null>(null);

    /**
     * Fetches usage summary from the backend API
     */
    getSummary(): Observable<UsageSummary> {
        this.loading.set(true);
        this.error.set(null);

        return this.#http.get<UsageApiResponse>(`${this.#BASE_URL}/summary`).pipe(
            map((response) => response.entity),
            tap((summary) => {
                this.summary.set(summary);
                this.loading.set(false);
            }),
            catchError((error) => {
                const errorMessage = this.getErrorMessage(error);
                this.error.set(errorMessage);
                this.errorStatus.set(error.status || null);
                this.loading.set(false);
                console.error('Failed to fetch usage summary:', error);
                throw error;
            })
        );
    }

    /**
     * Refreshes the usage data
     */
    refresh(): Observable<UsageSummary> {
        return this.getSummary();
    }

    /**
     * Resets the service state
     */
    reset(): void {
        this.summary.set(null);
        this.loading.set(false);
        this.error.set(null);
        this.errorStatus.set(null);
    }

    /**
     * Extracts user-friendly error message i18n key from HTTP error
     * Returns i18n keys that should be translated using the dm pipe in the component
     */
    private getErrorMessage(error: HttpErrorResponse | UsageErrorResponse): string {
        if (error.error?.message) {
            return error.error.message;
        }

        if (error.status) {
            switch (error.status) {
                case 401:
                    return 'usage.dashboard.error.unauthorized';
                case 403:
                    return 'usage.dashboard.error.forbidden';
                case 404:
                    return 'usage.dashboard.error.notFound';
                case 408:
                    return 'usage.dashboard.error.timeout';
                case 500:
                    return 'usage.dashboard.error.serverError';
                case 502:
                    return 'usage.dashboard.error.badGateway';
                case 503:
                    return 'usage.dashboard.error.serviceUnavailable';
                default:
                    // Return the i18n key - the component will handle parameter interpolation
                    return 'usage.dashboard.error.requestFailed';
            }
        }

        return 'usage.dashboard.error.generic';
    }
}
