import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

export interface GrafanaDashboard {
    id: number;
    uid: string;
    title: string;
    uri: string;
    url: string;
    slug: string;
    type: string;
    tags: string[];
    isStarred: boolean;
    folderId: string;
    folderUid: string;
    folderTitle: string;
    folderUrl: string;
}

export interface GrafanaFolder {
    id: number;
    uid: string;
    title: string;
    url: string;
    hasAcl: boolean;
    canSave: boolean;
    canEdit: boolean;
    canAdmin: boolean;
    createdBy: string;
    updatedBy: string;
    created: string;
    updated: string;
    version: number;
}

export interface DashboardDetail {
    dashboard: Record<string, unknown>;
    meta: {
        type: string;
        canSave: boolean;
        canEdit: boolean;
        canAdmin: boolean;
        canStar: boolean;
        slug: string;
        url: string;
        expires: number;
        created: Date;
        updated: Date;
        updatedBy: string;
        createdBy: string;
        version: number;
        hasAcl: boolean;
        isFolder: boolean;
        folderId: string;
        folderUid: string;
        folderTitle: string;
        folderUrl: string;
        provisioned: boolean;
    };
}

export interface ApiResponse<T> {
    entity: T;
    errors: unknown[];
    i18nMessagesMap: Record<string, string>;
    messages: unknown[];
    pagination: unknown;
    permissions: string[];
}

@Injectable({
    providedIn: 'root'
})
export class GrafanaService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = '/api/v1/grafana';

    /**
     * Test connectivity to Grafana
     */
    testConnection(): Observable<boolean> {
        return this.http.get<ApiResponse<boolean>>(`${this.baseUrl}/test-connection`)
            .pipe(
                map(response => response.entity)
            );
    }

    /**
     * Search for dashboards with optional filters
     */
    searchDashboards(params: {
        query?: string;
        type?: string;
        starred?: boolean;
        folderIds?: string;
        tag?: string;
        limit?: number;
    } = {}): Observable<GrafanaDashboard[]> {
        let httpParams = new HttpParams();

        if (params.query) {
            httpParams = httpParams.set('query', params.query);
        }
        if (params.type) {
            httpParams = httpParams.set('type', params.type);
        }
        if (params.starred !== undefined) {
            httpParams = httpParams.set('starred', params.starred.toString());
        }
        if (params.folderIds) {
            httpParams = httpParams.set('folderIds', params.folderIds);
        }
        if (params.tag) {
            httpParams = httpParams.set('tag', params.tag);
        }
        if (params.limit) {
            httpParams = httpParams.set('limit', params.limit.toString());
        }

        return this.http.get<ApiResponse<GrafanaDashboard[]>>(`${this.baseUrl}/dashboards/search`, { params: httpParams })
            .pipe(
                map(response => response.entity || [])
            );
    }

    /**
     * Get dashboard details by UID
     */
    getDashboardByUid(uid: string): Observable<DashboardDetail> {
        return this.http.get<ApiResponse<DashboardDetail>>(`${this.baseUrl}/dashboards/${uid}`)
            .pipe(
                map(response => response.entity)
            );
    }

    /**
     * Get all folders
     */
    getFolders(limit?: number): Observable<GrafanaFolder[]> {
        let httpParams = new HttpParams();
        if (limit) {
            httpParams = httpParams.set('limit', limit.toString());
        }

        return this.http.get<ApiResponse<GrafanaFolder[]>>(`${this.baseUrl}/folders`, { params: httpParams })
            .pipe(
                map(response => response.entity || [])
            );
    }

    /**
     * Get folder by UID
     */
    getFolderByUid(uid: string): Observable<GrafanaFolder> {
        return this.http.get<ApiResponse<GrafanaFolder>>(`${this.baseUrl}/folders/${uid}`)
            .pipe(
                map(response => response.entity)
            );
    }

    /**
     * Get dashboards in a specific folder
     */
    getDashboardsInFolder(folderUid: string): Observable<GrafanaDashboard[]> {
        return this.http.get<ApiResponse<GrafanaDashboard[]>>(`${this.baseUrl}/folders/${folderUid}/dashboards`)
            .pipe(
                map(response => response.entity || [])
            );
    }

    /**
     * Build Grafana dashboard URL for iframe embedding
     * This constructs the proper URL to embed a Grafana dashboard
     */
    buildDashboardUrl(dashboardUid: string, options: {
        theme?: 'light' | 'dark';
        kiosk?: boolean;
        autofitpanels?: boolean;
        from?: string;
        to?: string;
        refresh?: string;
        orgId?: number;
        panelId?: number;
        useProxy?: boolean;
    } = {}): string {
        // Determine base URL based on proxy option
        let baseUrl: string;

        if (options.useProxy) {
            // Use the dotCMS proxy URL
            baseUrl = 'http://localhost:8080/grafana-proxy';
        } else {
            // Use direct Grafana URL
            baseUrl = 'http://localhost:3000'; // This should come from config
        }

        let url = `${baseUrl}/d/${dashboardUid}`;

        const queryParams: string[] = [];

        // Add theme parameter
        if (options.theme) {
            queryParams.push(`theme=${options.theme}`);
        }

        // Add kiosk mode for clean embedding
        if (options.kiosk !== false) {
            queryParams.push('kiosk=1');
        }

        // Auto-fit panels to available space
        if (options.autofitpanels !== false) {
            queryParams.push('autofitpanels=1');
        }

        // Time range
        if (options.from) {
            queryParams.push(`from=${encodeURIComponent(options.from)}`);
        }
        if (options.to) {
            queryParams.push(`to=${encodeURIComponent(options.to)}`);
        }

        // Refresh interval
        if (options.refresh) {
            queryParams.push(`refresh=${options.refresh}`);
        }

        // Organization ID
        if (options.orgId) {
            queryParams.push(`orgId=${options.orgId}`);
        }

        // Specific panel ID (for single panel view)
        if (options.panelId) {
            queryParams.push(`panelId=${options.panelId}`);
            queryParams.push('viewPanel=1'); // Enable panel view mode
        }

        // Append query parameters
        if (queryParams.length > 0) {
            url += '?' + queryParams.join('&');
        }

        return url;
    }
}