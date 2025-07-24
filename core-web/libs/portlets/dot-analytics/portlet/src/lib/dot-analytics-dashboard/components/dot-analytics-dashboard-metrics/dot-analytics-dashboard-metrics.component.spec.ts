import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsDashboardMetricsComponent } from './dot-analytics-dashboard-metrics.component';

describe('DotAnalyticsDashboardMetricsComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardMetricsComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardMetricsComponent,
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent();

        // Setup the mock return value
        const messageService = spectator.inject(DotMessageService);
        messageService.get.mockReturnValue('Translated Message');
    });

    describe('Component Inputs', () => {
        it('should display metric name, value and subtitle', () => {
            // Arrange
            spectator.setInput('name', 'analytics.metrics.total-pageviews');
            spectator.setInput('value', 12345);
            spectator.setInput('subtitle', 'analytics.metrics.subtitle');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));
            const subtitle = spectator.query(byTestId('metric-subtitle'));

            expect(title).toBeTruthy();
            expect(value).toHaveText('12,345');
            expect(subtitle).toBeTruthy();
        });

        it('should format large numbers with locale separators', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 1234567);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const value = spectator.query(byTestId('metric-value'));
            expect(value).toHaveText('1,234,567');
        });

        it('should display icon when provided', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('icon', 'pi-eye');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const icon = spectator.query(byTestId('metric-icon'));
            expect(icon).toBeTruthy();
            expect(icon).toHaveClass('pi-eye');
        });

        it('should not display icon when not provided', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const icon = spectator.query(byTestId('metric-icon'));
            expect(icon).toBeFalsy();
        });

        it('should not display subtitle when not provided', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const subtitle = spectator.query(byTestId('metric-subtitle'));
            expect(subtitle).toBeFalsy();
        });
    });

    describe('Component States', () => {
        it('should show loading state with skeletons when status is INIT', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();

            // Act & Assert
            const skeletons = spectator.queryAll('p-skeleton');
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));

            expect(skeletons.length).toBeGreaterThan(0);
            expect(title).toBeFalsy();
            expect(value).toBeFalsy();
        });

        it('should show loading state with skeletons when status is LOADING', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            // Act & Assert
            const skeletons = spectator.queryAll('p-skeleton');
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));

            expect(skeletons.length).toBeGreaterThan(0);
            expect(title).toBeFalsy();
            expect(value).toBeFalsy();
        });

        it('should show error state when status is ERROR', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();

            // Act & Assert
            const errorIcon = spectator.query('.pi-exclamation-triangle');
            const errorMessage = spectator.query('.metric-error');
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));

            expect(errorIcon).toBeTruthy();
            expect(errorMessage).toBeTruthy();
            expect(title).toBeFalsy();
            expect(value).toBeFalsy();
        });

        it('should show normal content when status is LOADED', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));
            const skeletons = spectator.queryAll('p-skeleton');
            const errorIcon = spectator.query('.pi-exclamation-triangle');

            expect(title).toBeTruthy();
            expect(value).toBeTruthy();
            expect(skeletons.length).toBe(0);
            expect(errorIcon).toBeFalsy();
        });
    });

    describe('Computed Properties', () => {
        it('should detect loading state correctly for INIT and LOADING', () => {
            // INIT
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();
            expect(spectator.component['$isLoading']()).toBe(true);

            // LOADING
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();
            expect(spectator.component['$isLoading']()).toBe(true);

            // LOADED
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('should detect error state correctly', () => {
            // ERROR
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();
            expect(spectator.component['$isError']()).toBe(true);

            // LOADED
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.component['$isError']()).toBe(false);
        });

        it('should detect empty state correctly', () => {
            // Empty when value is 0
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Empty when value is null
            spectator.setInput('value', null);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Empty when value is undefined
            spectator.setInput('value', undefined);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Not empty when value is valid
            spectator.setInput('value', 100);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(false);

            // Not empty when loading (even if value is 0)
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(false);

            // Not empty when error (even if value is 0)
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(false);
        });

        it('should generate correct icon classes', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('icon', 'pi-eye');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const iconClasses = spectator.component['$iconClasses']();
            expect(iconClasses).toBe('pi pi-eye ');
        });
    });

    describe('Empty State', () => {
        it('should show empty state when value is 0', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const emptyState = spectator.query('[data-testid="metric-empty-state"]');
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));

            expect(emptyState).toExist();
            expect(title).not.toExist();
            expect(value).not.toExist();
        });

        it('should show empty state icon and message', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const emptyIcon = spectator.query('.pi-info-circle');
            const emptyMessage = spectator.query('.metric-empty');

            expect(emptyIcon).toExist();
            expect(emptyMessage).toExist();
        });

        it('should not show empty state when value is valid', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const emptyState = spectator.query('[data-testid="metric-empty-state"]');
            expect(emptyState).not.toExist();
        });
    });

    describe('Error State Updates', () => {
        it('should show updated error state with correct icon and message', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();

            const errorState = spectator.query('[data-testid="metric-error-state"]');
            const errorIcon = spectator.query('.pi-exclamation-triangle');
            const errorMessage = spectator.query('.metric-error');

            expect(errorState).toExist();
            expect(errorIcon).toExist();
            expect(errorIcon).toHaveClass('text-gray-400');
            expect(errorMessage).toExist();
        });
    });

    describe('Accessibility', () => {
        it('should have proper test ids for testing', () => {
            // Arrange
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('subtitle', 'test.subtitle');
            spectator.setInput('icon', 'pi-eye');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            expect(spectator.query(byTestId('metric-card'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-title'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-value'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-subtitle'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-icon'))).toBeTruthy();
        });
    });
});
