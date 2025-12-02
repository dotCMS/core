import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';

import { catchError, map, tap } from 'rxjs/operators';

/**
 * Content-related metrics for the usage dashboard
 */
export interface ContentMetrics {
    readonly totalContent: number;
    readonly contentTypes: number;
    readonly recentlyEdited: number;
    readonly contentTypesWithWorkflows: number;
    readonly lastContentEdited: string;
}

/**
 * Site-related metrics for the usage dashboard
 */
export interface SiteMetrics {
    readonly totalSites: number;
    readonly activeSites: number;
    readonly templates: number;
    readonly siteAliases: number;
}

/**
 * User activity metrics for the usage dashboard
 */
export interface UserMetrics {
    readonly activeUsers: number;
    readonly totalUsers: number;
    readonly recentLogins: number;
    readonly lastLogin: string;
}

/**
 * System configuration metrics for the usage dashboard
 */
export interface SystemMetrics {
    readonly languages: number;
    readonly workflowSchemes: number;
    readonly workflowSteps: number;
    readonly liveContainers: number;
    readonly builderTemplates: number;
}

/**
 * Complete usage summary containing all metric categories
 */
export interface UsageSummary {
    readonly contentMetrics: ContentMetrics;
    readonly siteMetrics: SiteMetrics;
    readonly userMetrics: UserMetrics;
    readonly systemMetrics: SystemMetrics;
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
    }

    /**
     * Extracts user-friendly error message from HTTP error
     */
    private getErrorMessage(error: HttpErrorResponse | UsageErrorResponse): string {
        if (error.error?.message) {
            return error.error.message;
        }

        if (error.status) {
            switch (error.status) {
                case 401:
                    return 'You are not authorized to view this data.';
                case 403:
                    return 'You do not have permission to access usage data.';
                case 404:
                    return 'Usage data not found.';
                case 408:
                    return 'Request timed out. Please try again.';
                case 500:
                    return 'Server error occurred. Please try again later.';
                case 502:
                    return 'Bad gateway. Please try again later.';
                case 503:
                    return 'Service unavailable. Please try again later.';
                default:
                    return `Request failed with status ${error.status}.`;
            }
        }

        return 'Failed to load usage data. Please check your connection and try again.';
    }
}
