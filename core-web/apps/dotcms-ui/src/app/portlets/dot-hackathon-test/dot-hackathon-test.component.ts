import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import {
    GrafanaService,
    GrafanaDashboard,
    GrafanaFolder
} from './services/grafana.service';
import { finalize, catchError, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { of, Subject } from 'rxjs';

type ConnectionStatus = 'loading' | 'connected' | 'disconnected' | null;

interface DashboardOptions {
    theme: 'light' | 'dark';
    kiosk: boolean;
    autofitpanels: boolean;
    refresh: string;
    dashboardUid?: string;
    from?: string;
    to?: string;
    orgId?: number;
    panelId?: number;
}

@Component({
    selector: 'dot-dot-hackathon-test',
    imports: [CommonModule, FormsModule, HttpClientModule],
    templateUrl: './dot-hackathon-test.component.html',
    styleUrl: './dot-hackathon-test.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHackathonTestComponent implements OnInit {
    private readonly sanitizer = inject(DomSanitizer);
    private readonly grafanaService = inject(GrafanaService);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly searchSubject = new Subject<string>();

    // Connection Status
    connectionStatus: ConnectionStatus = null;

    // Dashboard Data
    dashboards: GrafanaDashboard[] = [];
    folders: GrafanaFolder[] = [];
    selectedDashboard: GrafanaDashboard | null = null;
    selectedFolderId = '';

    // Search and Loading States
    searchQuery = '';
    searchLoading = false;
    searchPerformed = false;
    iframeLoading = false;

    // Dashboard Options
    dashboardOptions: DashboardOptions = {
        theme: 'light',
        kiosk: true,
        autofitpanels: true,
        refresh: ''
    };

    // Dashboard Display
    iFrameUrl: SafeResourceUrl | null = null;
    currentDashboardUrl = '';

    // Collapsible sections state
    isConnectionStatusCollapsed = false;
    isDashboardSelectionCollapsed = false;
    isDashboardOptionsCollapsed = false;

    ngOnInit() {
        this.initializeSearchDebounce();
        this.testConnection();
        this.loadFolders();
    }

    /**
     * Initialize search input debouncing
     */
    private initializeSearchDebounce() {
        this.searchSubject.pipe(
            debounceTime(300),
            distinctUntilChanged()
        ).subscribe(() => {
            this.searchDashboards();
        });
    }

    /**
     * Test connection to Grafana
     */
    testConnection() {
        console.log('🚀 === USER ACTION: Test Grafana Connection ===');
        console.log('🚀 Timestamp:', new Date().toISOString());

        this.connectionStatus = 'loading';
        this.cdr.detectChanges();

        this.grafanaService.testConnection()
            .pipe(
                catchError((error) => {
                    console.error('Grafana connection test failed:', error);
                    return of(false);
                }),
                finalize(() => {
                    this.cdr.detectChanges();
                })
            )
            .subscribe({
                next: (isConnected) => {
                    this.connectionStatus = isConnected ? 'connected' : 'disconnected';
                    if (isConnected) {
                        this.loadInitialDashboards();
                    }
                },
                error: (error) => {
                    console.error('Connection test error:', error);
                    this.connectionStatus = 'disconnected';
                }
            });
    }

    /**
     * Load folders for filtering
     */
    private loadFolders() {
        if (this.connectionStatus !== 'connected') {
            return;
        }

        this.grafanaService.getFolders(50)
            .pipe(
                catchError((error) => {
                    console.error('Error loading folders:', error);
                    return of([]);
                })
            )
            .subscribe({
                next: (folders) => {
                    this.folders = folders;
                    this.cdr.detectChanges();
                }
            });
    }

    /**
     * Load initial set of dashboards
     */
    private loadInitialDashboards() {
        this.searchDashboards();
    }

    /**
     * Handle search input changes
     */
    onSearchChange() {
        this.searchSubject.next(this.searchQuery);
    }

    /**
     * Handle folder selection change
     */
    onFolderChange() {
        this.searchDashboards();
    }

    /**
     * Search dashboards based on current filters
     */
    searchDashboards() {
        if (this.connectionStatus !== 'connected') {
            return;
        }

        this.searchLoading = true;
        this.cdr.detectChanges();

        const searchParams = {
            query: this.searchQuery || undefined,
            type: 'dash-db',
            limit: 50,
            folderIds: this.selectedFolderId || undefined
        };

        this.grafanaService.searchDashboards(searchParams)
            .pipe(
                catchError((error) => {
                    console.error('Error searching dashboards:', error);
                    return of([]);
                }),
                finalize(() => {
                    this.searchLoading = false;
                    this.searchPerformed = true;
                    this.cdr.detectChanges();
                })
            )
            .subscribe({
                next: (dashboards) => {
                    this.dashboards = dashboards;
                    console.log(`Found ${dashboards.length} dashboards`);
                }
            });
    }

    /**
     * Select a dashboard for display
     */
    selectDashboard(dashboard: GrafanaDashboard) {
        console.log('🎯 === USER ACTION: Select Dashboard ===');
        console.log('🎯 Dashboard Title:', dashboard.title);
        console.log('🎯 Dashboard UID:', dashboard.uid);
        console.log('🎯 Dashboard Type:', dashboard.type);
        console.log('🎯 Dashboard Folder:', dashboard.folderTitle);
        console.log('🎯 Dashboard Tags:', dashboard.tags);
        console.log('🎯 Timestamp:', new Date().toISOString());

        this.selectedDashboard = dashboard;

        // Set the dashboard UID in options automatically
        this.dashboardOptions = {
            ...this.dashboardOptions,
            dashboardUid: dashboard.uid
        };

        console.log('🎯 Updated Dashboard Options:', this.dashboardOptions);

        this.cdr.detectChanges();
    }

    /**
     * Load the selected dashboard in the iframe
     */
    loadDashboard() {
        if (!this.selectedDashboard) {
            return;
        }

        this.iframeLoading = true;
        this.cdr.detectChanges();

        // Build the dashboard URL with current options
        this.currentDashboardUrl = this.grafanaService.buildDashboardUrl(
            this.selectedDashboard.uid,
            this.dashboardOptions
        );

        // Sanitize and set the URL for the iframe
        this.iFrameUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.currentDashboardUrl);
        this.cdr.detectChanges();

        // Auto-collapse dashboard selection when loading dashboard
        this.isDashboardSelectionCollapsed = true;

        // Auto-scroll to dashboard after a short delay to let it render
        setTimeout(() => {
            this.scrollToDashboard();
        }, 500);

        console.log('Loading dashboard URL:', this.currentDashboardUrl);
    }

    /**
     * Scroll to dashboard display section
     */
    private scrollToDashboard() {
        const dashboardElement = document.querySelector('.dashboard-display');
        if (dashboardElement) {
            dashboardElement.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    }

    /**
     * Refresh the current dashboard
     */
    refreshDashboard() {
        if (!this.selectedDashboard || !this.iFrameUrl) {
            return;
        }

        this.iframeLoading = true;
        this.cdr.detectChanges();

        // Force iframe reload by updating the URL
        const timestamp = Date.now();
        const separator = this.currentDashboardUrl.includes('?') ? '&' : '?';
        const refreshUrl = `${this.currentDashboardUrl}${separator}_t=${timestamp}`;

        this.iFrameUrl = this.sanitizer.bypassSecurityTrustResourceUrl(refreshUrl);
        this.cdr.detectChanges();
    }

    /**
     * Open dashboard in fullscreen (new tab)
     */
    fullscreenDashboard() {
        if (!this.selectedDashboard) {
            return;
        }

        // Build fullscreen URL without kiosk mode
        const fullscreenOptions = {
            ...this.dashboardOptions,
            kiosk: false
        };

        const fullscreenUrl = this.grafanaService.buildDashboardUrl(
            this.selectedDashboard.uid,
            fullscreenOptions
        );

        window.open(fullscreenUrl, '_blank');
    }

    /**
     * Handle iframe load event
     */
    onIframeLoad() {
        this.iframeLoading = false;
        this.cdr.detectChanges();
        console.log('Dashboard loaded successfully');
    }

    /**
     * Handle iframe error event
     */
    onIframeError() {
        this.iframeLoading = false;
        this.cdr.detectChanges();
        console.error('Error loading dashboard iframe');
    }

    /**
     * Toggle connection status section
     */
    toggleConnectionStatus() {
        this.isConnectionStatusCollapsed = !this.isConnectionStatusCollapsed;
        this.cdr.detectChanges();
    }

    /**
     * Toggle dashboard selection section
     */
    toggleDashboardSelection() {
        this.isDashboardSelectionCollapsed = !this.isDashboardSelectionCollapsed;
        this.cdr.detectChanges();
    }

    /**
     * Toggle dashboard options section
     */
    toggleDashboardOptions() {
        this.isDashboardOptionsCollapsed = !this.isDashboardOptionsCollapsed;
        this.cdr.detectChanges();
    }
}
