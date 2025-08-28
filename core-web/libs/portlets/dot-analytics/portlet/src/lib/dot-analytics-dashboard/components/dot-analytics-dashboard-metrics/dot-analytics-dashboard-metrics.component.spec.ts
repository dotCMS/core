import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsDashboardMetricsComponent } from './dot-analytics-dashboard-metrics.component';

describe('DotAnalyticsDashboardMetricsComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardMetricsComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardMetricsComponent,
        imports: [NoopAnimationsModule],
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                name: 'test.metric',
                value: 100,
                status: ComponentStatus.LOADED
            } as unknown,
            detectChanges: false
        });

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
            const errorIcon = spectator.query('.pi.pi-exclamation-triangle');
            const errorMessage = spectator.query('.state-message');
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));

            expect(errorIcon).toBeTruthy();
            expect(errorMessage).toBeTruthy();
            expect(title).toBeTruthy(); // Title is shown in error state
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
            // Not empty when value is 0 (zero is a valid metric)
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.component['$isEmpty']()).toBe(false);

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
            expect(iconClasses).toBe('pi pi-eye');
        });
    });

    describe('Empty State', () => {
        it('should NOT show empty state when value is 0 (0 is a valid metric value)', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const emptyIcon = spectator.query('.pi.pi-info-circle');
            const title = spectator.query(byTestId('metric-title'));
            const value = spectator.query(byTestId('metric-value'));

            expect(emptyIcon).not.toExist();
            expect(title).toExist();
            expect(value).toExist();
            expect(value).toHaveText('0'); // 0 should be displayed as a valid value
        });

        it('should show empty state icon and message when value is null', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', null);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const emptyIcon = spectator.query('.pi.pi-info-circle');
            const emptyMessage = spectator.query('.state-message');

            expect(emptyIcon).toExist();
            expect(emptyMessage).toExist();
        });

        it('should not show empty state when value is valid', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const emptyIcon = spectator.query('.pi.pi-info-circle');
            expect(emptyIcon).not.toExist();
        });
    });

    describe('Error State Updates', () => {
        it('should show updated error state with correct icon and message', () => {
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();

            const errorIcon = spectator.query('.pi.pi-exclamation-triangle');
            const errorMessage = spectator.query('.state-message');

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

    describe('Animations', () => {
        it('should have animation triggers configured', () => {
            // Arrange & Act
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Assert - Since we're using NoopAnimationsModule, we can't test the actual animation behavior
            // but we can verify the component renders correctly with animations configured
            expect(spectator.component).toBeTruthy();
            expect(spectator.query(byTestId('metric-value'))).toBeTruthy();
        });

        it('should render correctly during state transitions with animations', () => {
            // Arrange - Start with loading state
            spectator.setInput('name', 'test.metric');
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            // Assert loading state
            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);

            // Act - Change to loaded state
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Assert loaded state
            expect(spectator.query(byTestId('metric-value'))).toBeTruthy();
            expect(spectator.queryAll('p-skeleton').length).toBe(0);
        });
    });
});
