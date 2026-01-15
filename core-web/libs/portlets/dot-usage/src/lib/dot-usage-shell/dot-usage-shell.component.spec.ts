import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';
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

    const createMockService = () => ({
        getSummary: jest.fn().mockReturnValue(of(mockSummary)),
        refresh: jest.fn().mockReturnValue(of(mockSummary)),
        getErrorMessage: jest.fn().mockReturnValue('usage.dashboard.error.generic')
    });

    let mockService: ReturnType<typeof createMockService>;

    const createComponent = createComponentFactory({
        component: DotUsageShellComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: DotUsageService,
                useFactory: () => mockService
            }
        ]
    });

    beforeEach(() => {
        // Create a fresh mock service for each test
        mockService = createMockService();
        // Mock console.error to avoid noise in test output
        jest.spyOn(console, 'error').mockImplementation(() => {});
        spectator = createComponent();
        usageService = spectator.inject(DotUsageService);
    });

    afterEach(() => {
        // Restore console.error after each test
        jest.restoreAllMocks();
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

        // Check for skeleton components in loading state
        expect(spectator.query('p-skeleton')).toBeTruthy();
    });

    it('should display error state', () => {
        const errorMessage = 'usage.dashboard.error.generic';
        spectator.component.loading.set(false);
        spectator.component.error.set(errorMessage);

        spectator.detectChanges();

        // Check for error card and retry button
        const errorCard = spectator.query('p-card');
        expect(errorCard).toBeTruthy();
        expect(spectator.query(byTestId('retry-button'))).toBeTruthy();
    });

    it('should display data when loaded', () => {
        spectator.component.loading.set(false);
        spectator.component.summary.set(mockSummary);
        spectator.component.error.set(null);

        spectator.detectChanges();

        // Check for metric cards
        expect(spectator.query(byTestId('site-COUNT_OF_SITES-card'))).toBeTruthy();
        expect(spectator.query(byTestId('content-COUNT_CONTENT-card'))).toBeTruthy();
        expect(spectator.query(byTestId('user-COUNT_OF_USERS-card'))).toBeTruthy();
        expect(spectator.query(byTestId('system-COUNT_LANGUAGES-card'))).toBeTruthy();
    });

    it('should handle refresh button click', () => {
        // Reset call count before test
        (usageService.getSummary as jest.Mock).mockClear();
        const refreshButton = spectator.query(byTestId('refresh-button'));
        expect(refreshButton).toBeTruthy();

        // PrimeNG buttons use onClick event, not native click
        spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', new MouseEvent('click'));
        spectator.detectChanges();
        
        expect(usageService.getSummary).toHaveBeenCalled();
    });

    it('should handle retry button click', () => {
        spectator.component.loading.set(false);
        spectator.component.error.set('Some error');
        spectator.component.summary.set(mockSummary);
        spectator.detectChanges();

        const retryButton = spectator.query(byTestId('retry-button'));
        expect(retryButton).toBeTruthy();

        // Use a Subject to control when the observable emits
        const summarySubject = new Subject<UsageSummary>();
        (usageService.getSummary as jest.Mock).mockClear();
        (usageService.getSummary as jest.Mock).mockReturnValue(summarySubject.asObservable());

        // PrimeNG buttons use onClick event, not native click
        spectator.triggerEventHandler('[data-testid="retry-button"]', 'onClick', new MouseEvent('click'));
        spectator.detectChanges();

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
        const httpError: { status: number; statusText: string } = {
            status: 500,
            statusText: 'Internal Server Error'
        };
        (usageService.getSummary as jest.Mock).mockReturnValue(throwError(() => httpError));
        (usageService.getErrorMessage as jest.Mock).mockReturnValue('usage.dashboard.error.serverError');

        // console.error is already mocked in beforeEach, but we can verify it was called
        const errorCallCount = (console.error as jest.Mock).mock.calls.length;

        spectator.component.loadData();

        // Verify console.error was called (the mock from beforeEach should have been called)
        expect(console.error).toHaveBeenCalled();
        expect((console.error as jest.Mock).mock.calls[errorCallCount][0]).toBe('Failed to load usage data:');
        expect(spectator.component.error()).toBe('usage.dashboard.error.serverError');
        expect(spectator.component.loading()).toBe(false);
    });

    it('should show correct metric values', () => {
        spectator.component.summary.set(mockSummary);
        spectator.detectChanges();

        // Query the metric value from the card body (p tag inside p-card)
        const siteCard = spectator.query(byTestId('site-COUNT_OF_SITES-card'));
        const siteMetricValue = siteCard?.querySelector('p');
        expect(siteMetricValue?.textContent?.trim()).toBe('5');

        const contentCard = spectator.query(byTestId('content-COUNT_CONTENT-card'));
        const contentMetricValue = contentCard?.querySelector('p');
        expect(contentMetricValue?.textContent?.trim()).toBe('1.5K');
    });

    it('should display analytics message', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('analytics-message'))).toBeTruthy();
        expect(spectator.query(byTestId('message-content'))).toBeTruthy();
    });

    it('should display last updated timestamp when data is loaded', () => {
        spectator.component.loading.set(false);
        spectator.component.summary.set(mockSummary);
        spectator.component.lastUpdated.set(new Date('2024-01-15T15:30:00Z'));
        spectator.detectChanges();

        // Check that last updated is displayed in toolbar
        const toolbar = spectator.query('p-toolbar');
        expect(toolbar).toBeTruthy();
    });

    it('should format metric value correctly for string values', () => {
        const stringValue = 'N/A';
        expect(spectator.component.formatMetricValue(stringValue)).toBe('N/A');
    });

    it('should format metric value correctly for numeric values', () => {
        expect(spectator.component.formatMetricValue(1500)).toBe('1.5K');
        expect(spectator.component.formatMetricValue(1500000)).toBe('1.5M');
        expect(spectator.component.formatMetricValue(500)).toBe('500');
    });

    it('should return not available for undefined or null values', () => {
        expect(spectator.component.formatMetricValue(undefined)).toBe(
            'usage.dashboard.value.notAvailable'
        );
        expect(spectator.component.formatMetricValue(null)).toBe(
            'usage.dashboard.value.notAvailable'
        );
    });

    it('should check if value is string correctly', () => {
        expect(spectator.component.isStringValue('N/A')).toBe(true);
        expect(spectator.component.isStringValue('1500')).toBe(false); // numeric string
        expect(spectator.component.isStringValue(1500)).toBe(false);
        expect(spectator.component.isStringValue(undefined)).toBe(false);
        expect(spectator.component.isStringValue(null)).toBe(false);
    });

    it('should get metric by category and name', () => {
        spectator.component.summary.set(mockSummary);

        const metric = spectator.component.getMetric('content', 'COUNT_CONTENT');
        expect(metric).toBeDefined();
        expect(metric?.name).toBe('COUNT_CONTENT');
        expect(metric?.value).toBe(1500);
    });

    it('should return undefined for non-existent metric', () => {
        spectator.component.summary.set(mockSummary);

        const metric = spectator.component.getMetric('content', 'NON_EXISTENT');
        expect(metric).toBeUndefined();
    });

    it('should get category metrics', () => {
        spectator.component.summary.set(mockSummary);

        const categoryMetrics = spectator.component.getCategoryMetrics('content');
        expect(categoryMetrics).toBeDefined();
        expect(categoryMetrics?.['COUNT_CONTENT']).toBeDefined();
    });

    it('should get all categories', () => {
        spectator.component.summary.set(mockSummary);

        const categories = spectator.component.getCategories();
        expect(categories).toContain('content');
        expect(categories).toContain('site');
        expect(categories).toContain('user');
        expect(categories).toContain('system');
    });

    it('should get category title key', () => {
        expect(spectator.component.getCategoryTitleKey('content')).toBe(
            'usage.category.content.title'
        );
        expect(spectator.component.getCategoryTitleKey('site')).toBe('usage.category.site.title');
    });

    it('should check if value is i18n key', () => {
        expect(spectator.component.isI18nKey('usage.dashboard.error.generic')).toBe(true);
        expect(spectator.component.isI18nKey('usage.dashboard.value.notAvailable')).toBe(true);
        expect(spectator.component.isI18nKey('regular-text')).toBe(false);
    });

    it('should unsubscribe on destroy', () => {
        spectator.component.ngOnInit();
        
        const subscription = spectator.component['dataSubscription'] as { unsubscribe: () => void } | undefined;
        if (subscription) {
            const unsubscribeSpy = jest.spyOn(subscription, 'unsubscribe');
            spectator.component.ngOnDestroy();
            expect(unsubscribeSpy).toHaveBeenCalled();
        }
    });

    it('should handle multiple refresh calls correctly', () => {
        // Reset call count before test
        (usageService.getSummary as jest.Mock).mockClear();
        
        const refreshButton = spectator.query(byTestId('refresh-button'));
        expect(refreshButton).toBeTruthy();
        
        // PrimeNG buttons use onClick event, not native click
        // Click refresh multiple times
        spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', new MouseEvent('click'));
        spectator.detectChanges();
        spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', new MouseEvent('click'));
        spectator.detectChanges();
        spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', new MouseEvent('click'));
        spectator.detectChanges();

        // Should call getSummary for each click
        expect(usageService.getSummary).toHaveBeenCalledTimes(3);
    });
});
