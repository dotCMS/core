import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

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
    dashboard: Record<string, any>;
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
    errors: any[];
    i18nMessagesMap: Record<string, string>;
    messages: any[];
    pagination: any;
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
        const url = `${this.baseUrl}/test-connection`;
        console.log('🔗 === GRAFANA REQUEST: Test Connection ===');
        console.log('📤 URL:', url);
        console.log('📤 Method: GET');
        console.log('📤 Timestamp:', new Date().toISOString());

        return this.http.get<ApiResponse<boolean>>(url)
            .pipe(
                map(response => {
                    console.log('📥 === GRAFANA RESPONSE: Test Connection ===');
                    console.log('📥 Status: SUCCESS');
                    console.log('📥 Full Response:', response);
                    console.log('📥 Connection Result:', response.entity);
                    console.log('📥 Timestamp:', new Date().toISOString());
                    return response.entity;
                }),
                catchError(error => {
                    console.error('❌ === GRAFANA ERROR: Test Connection ===');
                    console.error('❌ Error Details:', error);
                    console.error('❌ Status:', error.status);
                    console.error('❌ Message:', error.message);
                    console.error('❌ Timestamp:', new Date().toISOString());
                    throw error;
                })
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

        const url = `${this.baseUrl}/dashboards/search`;
        const fullUrl = httpParams.toString() ? `${url}?${httpParams.toString()}` : url;
        console.log('🔍 === GRAFANA REQUEST: Search Dashboards ===');
        console.log('📤 Base URL:', url);
        console.log('📤 Full Request URL:', fullUrl);
        console.log('📤 Method: GET');
        console.log('📤 Parameters:', params);
        console.log('📤 HTTP Params:', httpParams.toString());
        console.log('📤 Timestamp:', new Date().toISOString());

        return this.http.get<ApiResponse<GrafanaDashboard[]>>(url, { params: httpParams })
            .pipe(
                map(response => {
                    const dashboards = response.entity || [];
                    console.log('📥 === GRAFANA RESPONSE: Search Dashboards ===');
                    console.log('📥 Status: SUCCESS');
                    console.log('📥 Dashboards Count:', dashboards.length);
                    console.log('📥 Full Response:', response);
                    if (dashboards.length > 0) {
                        console.log('📥 Sample Dashboards:');
                        dashboards.slice(0, 3).forEach((dashboard, index) => {
                            console.log(`📥   [${index + 1}] ${dashboard.title} (UID: ${dashboard.uid}, Type: ${dashboard.type})`);
                        });
                    }
                    console.log('📥 Timestamp:', new Date().toISOString());
                    return dashboards;
                }),
                catchError(error => {
                    console.error('❌ === GRAFANA ERROR: Search Dashboards ===');
                    console.error('❌ Error Details:', error);
                    console.error('❌ Status:', error.status);
                    console.error('❌ Message:', error.message);
                    console.error('❌ Parameters:', params);
                    console.error('❌ Timestamp:', new Date().toISOString());
                    throw error;
                })
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

        const url = `${this.baseUrl}/folders`;
        const fullUrl = httpParams.toString() ? `${url}?${httpParams.toString()}` : url;
        console.log('📁 === GRAFANA REQUEST: Get Folders ===');
        console.log('📤 Base URL:', url);
        console.log('📤 Full Request URL:', fullUrl);
        console.log('📤 Method: GET');
        console.log('📤 Limit:', limit);
        console.log('📤 HTTP Params:', httpParams.toString());
        console.log('📤 Timestamp:', new Date().toISOString());

        return this.http.get<ApiResponse<GrafanaFolder[]>>(url, { params: httpParams })
            .pipe(
                map(response => {
                    const folders = response.entity || [];
                    console.log('📥 === GRAFANA RESPONSE: Get Folders ===');
                    console.log('📥 Status: SUCCESS');
                    console.log('📥 Folders Count:', folders.length);
                    console.log('📥 Full Response:', response);
                    if (folders.length > 0) {
                        console.log('📥 Sample Folders:');
                        folders.slice(0, 3).forEach((folder, index) => {
                            console.log(`📥   [${index + 1}] ${folder.title} (UID: ${folder.uid})`);
                        });
                    }
                    console.log('📥 Timestamp:', new Date().toISOString());
                    return folders;
                }),
                catchError(error => {
                    console.error('❌ === GRAFANA ERROR: Get Folders ===');
                    console.error('❌ Error Details:', error);
                    console.error('❌ Status:', error.status);
                    console.error('❌ Message:', error.message);
                    console.error('❌ Limit:', limit);
                    console.error('❌ Timestamp:', new Date().toISOString());
                    throw error;
                })
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
    } = {}): string {
        console.log('🔨 === GRAFANA URL BUILDER: Build Dashboard URL ===');
        console.log('📤 Dashboard UID:', dashboardUid);
        console.log('📤 Options:', options);
        console.log('📤 Timestamp:', new Date().toISOString());

        // Get base Grafana URL from configuration (this should match the grafana.api.url config)
        const baseGrafanaUrl = 'http://localhost:3000'; // This should come from config

        let url = `${baseGrafanaUrl}/d/${dashboardUid}`;
        console.log('📤 Base URL:', url);

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

        console.log('📤 Query Parameters:', queryParams);

        // Append query parameters
        if (queryParams.length > 0) {
            url += '?' + queryParams.join('&');
        }

        console.log('📥 === GRAFANA URL BUILDER RESULT ===');
        console.log('📥 Final URL:', url);
        console.log('📥 URL Length:', url.length);
        console.log('📥 Timestamp:', new Date().toISOString());

        return url;
    }
}