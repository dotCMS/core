import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { signal } from '@angular/core';

import { DotUsageShellComponent } from './dot-usage-shell.component';

import { DotUsageService, UsageSummary } from '../services/dot-usage.service';

describe('DotUsageShellComponent', () => {
    let spectator: Spectator<DotUsageShellComponent>;
    let usageService: DotUsageService;

    const mockSummary: UsageSummary = {
        contentMetrics: {
            totalContent: 1500,
            contentTypes: 25,
            recentlyEdited: 230,
            contentTypesWithWorkflows: 18,
            lastContentEdited: '2024-01-15'
        },
        siteMetrics: {
            totalSites: 5,
            activeSites: 4,
            templates: 12,
            siteAliases: 8
        },
        userMetrics: {
            activeUsers: 45,
            totalUsers: 60,
            recentLogins: 12,
            lastLogin: '2024-01-15T10:30:00Z'
        },
        systemMetrics: {
            languages: 3,
            workflowSchemes: 2,
            workflowSteps: 14,
            liveContainers: 9,
            builderTemplates: 6
        },
        lastUpdated: '2024-01-15T15:30:00Z'
    };

    const mockService = {
        summary: signal<UsageSummary | null>(null),
        loading: signal<boolean>(false),
        error: signal<string | null>(null),
        getSummary: jest.fn().mockReturnValue(of(mockSummary)),
        refresh: jest.fn().mockReturnValue(of(mockSummary)),
        reset: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotUsageShellComponent,
        imports: [HttpClientTestingModule],
        providers: [{ provide: DotUsageService, useValue: mockService }]
    });

    beforeEach(() => {
        spectator = createComponent();
        usageService = spectator.inject(DotUsageService);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should load data on init', () => {
        spectator.component.ngOnInit();
        expect(usageService.getSummary).toHaveBeenCalled();
    });

    it('should display loading state', () => {
        // Mock loading state
        usageService.loading.set(true);
        usageService.summary.set(null);

        spectator.detectChanges();

        expect(spectator.query('.usage-skeleton')).toBeTruthy();
        expect(spectator.query('p-skeleton')).toBeTruthy();
    });

    it('should display error state', () => {
        const errorMessage = 'Failed to load data';
        usageService.loading.set(false);
        usageService.error.set(errorMessage);

        spectator.detectChanges();

        expect(spectator.query('.usage-error')).toBeTruthy();
        expect(spectator.query('p-messages')).toBeTruthy();
        expect(spectator.query('[data-testid="retry-button"]')).toBeTruthy();
    });

    it('should display data when loaded', () => {
        usageService.loading.set(false);
        usageService.summary.set(mockSummary);
        usageService.error.set(null);

        spectator.detectChanges();

        expect(spectator.query('.usage-content')).toBeTruthy();
        expect(spectator.query('[data-testid="total-sites-card"]')).toBeTruthy();
        expect(spectator.query('[data-testid="total-content-card"]')).toBeTruthy();
        expect(spectator.query('[data-testid="total-users-card"]')).toBeTruthy();
        expect(spectator.query('[data-testid="languages-card"]')).toBeTruthy();
    });

    it('should handle refresh button click', () => {
        spectator.click('[data-testid="refresh-button"]');
        expect(usageService.getSummary).toHaveBeenCalled();
    });

    it('should handle retry button click', () => {
        usageService.loading.set(false);
        usageService.error.set('Some error');
        spectator.detectChanges();

        const retryButton = spectator.query('[data-testid="retry-button"]');
        expect(retryButton).toBeTruthy();

        // Reset the mocks to clear previous calls
        jest.clearAllMocks();

        spectator.click('[data-testid="retry-button"]');

        expect(usageService.reset).toHaveBeenCalled();
        expect(usageService.getSummary).toHaveBeenCalled();
    });

    it('should format numbers correctly', () => {
        expect(spectator.component.formatNumber(1500)).toBe('1.5K');
        expect(spectator.component.formatNumber(1500000)).toBe('1.5M');
        expect(spectator.component.formatNumber(500)).toBe('500');
    });

    it('should handle service errors gracefully', () => {
        const errorSpy = jest.spyOn(console, 'error').mockImplementation();
        usageService.getSummary = jest.fn().mockReturnValue(throwError(() => 'Network error'));

        spectator.component.loadData();

        expect(errorSpy).toHaveBeenCalledWith('Failed to load usage data:', 'Network error');
        errorSpy.mockRestore();
    });

    it('should show correct metric values', () => {
        usageService.summary.set(mockSummary);
        spectator.detectChanges();

        const totalSitesCard = spectator.query('[data-testid="total-sites-card"]');
        expect(totalSitesCard?.textContent).toContain('5');

        const totalContentCard = spectator.query('[data-testid="total-content-card"]');
        expect(totalContentCard?.textContent).toContain('1.5K');
    });
});
