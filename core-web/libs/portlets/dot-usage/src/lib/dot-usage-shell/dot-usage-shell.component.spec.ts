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
        metrics: {
            content: {
                COUNT_CONTENT: {
                    name: 'COUNT_CONTENT',
                    value: 1500,
                    displayLabel: 'usage.metric.COUNT_CONTENT'
                }
            },
            site: {
                COUNT_OF_SITES: {
                    name: 'COUNT_OF_SITES',
                    value: 5,
                    displayLabel: 'usage.metric.COUNT_OF_SITES'
                }
            },
            user: {
                COUNT_OF_USERS: {
                    name: 'COUNT_OF_USERS',
                    value: 60,
                    displayLabel: 'usage.metric.COUNT_OF_USERS'
                }
            },
            system: {
                COUNT_LANGUAGES: {
                    name: 'COUNT_LANGUAGES',
                    value: 3,
                    displayLabel: 'usage.metric.COUNT_LANGUAGES'
                }
            }
        },
        lastUpdated: '2024-01-15T15:30:00Z'
    };

    const mockService = {
        summary: signal<UsageSummary | null>(null),
        loading: signal<boolean>(false),
        error: signal<string | null>(null),
        errorStatus: signal<number | null>(null),
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

        expect(spectator.query('p-skeleton')).toBeTruthy();
    });

    it('should display error state', () => {
        const errorMessage = 'Failed to load data';
        usageService.loading.set(false);
        usageService.error.set(errorMessage);

        spectator.detectChanges();

        expect(spectator.query('[data-testid="retry-button"]')).toBeTruthy();
    });

    it('should display data when loaded', () => {
        usageService.loading.set(false);
        usageService.summary.set(mockSummary);
        usageService.error.set(null);

        spectator.detectChanges();

        expect(spectator.query('[data-testid="site-COUNT_OF_SITES-card"]')).toBeTruthy();
        expect(spectator.query('[data-testid="content-COUNT_CONTENT-card"]')).toBeTruthy();
        expect(spectator.query('[data-testid="user-COUNT_OF_USERS-card"]')).toBeTruthy();
        expect(spectator.query('[data-testid="system-COUNT_LANGUAGES-card"]')).toBeTruthy();
    });

    it('should handle refresh button click', () => {
        jest.clearAllMocks();
        const refreshButton = spectator.query('[data-testid="refresh-button"]');
        expect(refreshButton).toBeTruthy();

        spectator.dispatchFakeEvent(refreshButton, 'onClick');
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

        spectator.dispatchFakeEvent(retryButton, 'onClick');

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
        usageService.getSummary = jest.fn().mockReturnValue(throwError('Network error'));

        spectator.component.loadData();

        expect(errorSpy).toHaveBeenCalledWith('Failed to load usage data:', 'Network error');
        errorSpy.mockRestore();
    });

    it('should show correct metric values', () => {
        usageService.summary.set(mockSummary);
        spectator.detectChanges();

        const totalSitesCard = spectator.query('[data-testid="site-COUNT_OF_SITES-card"]');
        expect(totalSitesCard?.textContent).toContain('5');

        const totalContentCard = spectator.query('[data-testid="content-COUNT_CONTENT-card"]');
        expect(totalContentCard?.textContent).toContain('1.5K');
    });
});
