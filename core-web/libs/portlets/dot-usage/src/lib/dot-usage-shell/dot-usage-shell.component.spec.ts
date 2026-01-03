import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of, throwError, Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotUsageService, UsageSummary } from '@dotcms/data-access';

import { DotUsageShellComponent } from './dot-usage-shell.component';

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
        getSummary: jest.fn().mockReturnValue(of(mockSummary)),
        refresh: jest.fn().mockReturnValue(of(mockSummary)),
        getErrorMessage: jest.fn().mockReturnValue('usage.dashboard.error.generic')
    };

    const createComponent = createComponentFactory({
        component: DotUsageShellComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotUsageService, useValue: mockService }
        ]
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
        // Set component loading state
        spectator.component.loading.set(true);
        spectator.component.summary.set(null);

        spectator.detectChanges();

        expect(spectator.query('p-skeleton')).toBeTruthy();
    });

    it('should display error state', () => {
        const errorMessage = 'Failed to load data';
        spectator.component.loading.set(false);
        spectator.component.error.set(errorMessage);

        spectator.detectChanges();

        expect(spectator.query('[data-testid="retry-button"]')).toBeTruthy();
    });

    it('should display data when loaded', () => {
        spectator.component.loading.set(false);
        spectator.component.summary.set(mockSummary);
        spectator.component.error.set(null);

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

        spectator.click(refreshButton);
        expect(usageService.getSummary).toHaveBeenCalled();
    });

    it('should handle retry button click', () => {
        spectator.component.loading.set(false);
        spectator.component.error.set('Some error');
        spectator.component.summary.set(mockSummary);
        spectator.detectChanges();

        const retryButton = spectator.query('[data-testid="retry-button"]');
        expect(retryButton).toBeTruthy();

        // Reset the mocks to clear previous calls
        jest.clearAllMocks();

        // Use a Subject to control when the observable emits
        const summarySubject = new Subject<UsageSummary>();
        usageService.getSummary = jest.fn().mockReturnValue(summarySubject.asObservable());

        spectator.click(retryButton);

        // Check that reset happened synchronously (before observable completes)
        // The onRetry method resets state first, then calls loadData
        expect(spectator.component.summary()).toBeNull();
        expect(spectator.component.error()).toBeNull();
        expect(spectator.component.errorStatus()).toBeNull();
        expect(usageService.getSummary).toHaveBeenCalled();
        // After loadData is called, loading should be true
        expect(spectator.component.loading()).toBe(true);

        // Complete the observable
        summarySubject.next(mockSummary);
        summarySubject.complete();
    });

    it('should format numbers correctly', () => {
        expect(spectator.component.formatNumber(1500)).toBe('1.5K');
        expect(spectator.component.formatNumber(1500000)).toBe('1.5M');
        expect(spectator.component.formatNumber(500)).toBe('500');
    });

    it('should handle service errors gracefully', () => {
        const errorSpy = jest.spyOn(console, 'error').mockImplementation();
        const httpError: { status: number; statusText: string } = {
            status: 500,
            statusText: 'Internal Server Error'
        };
        usageService.getSummary = jest.fn().mockReturnValue(throwError(() => httpError));
        usageService.getErrorMessage = jest
            .fn()
            .mockReturnValue('usage.dashboard.error.serverError');

        spectator.component.loadData();

        // The error passed to console.error will be the error object, not the function
        expect(errorSpy).toHaveBeenCalled();
        expect(errorSpy.mock.calls[0][0]).toBe('Failed to load usage data:');
        expect(spectator.component.error()).toBe('usage.dashboard.error.serverError');
        expect(spectator.component.loading()).toBe(false);
        errorSpy.mockRestore();
    });

    it('should show correct metric values', () => {
        spectator.component.summary.set(mockSummary);
        spectator.detectChanges();

        const totalSitesMetric = spectator.query(
            '[data-testid="site-COUNT_OF_SITES-card"] .metric-value'
        );
        expect(totalSitesMetric?.textContent).toBe('5');

        const totalContentMetric = spectator.query(
            '[data-testid="content-COUNT_CONTENT-card"] .metric-value'
        );
        expect(totalContentMetric?.textContent).toBe('1.5K');
    });
});
