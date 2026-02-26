import {
    byTestId,
    createComponentFactory,
    createHostFactory,
    Spectator,
    SpectatorHost
} from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsMetricComponent } from './dot-analytics-metric.component';

describe('DotAnalyticsMetricComponent', () => {
    let spectator: Spectator<DotAnalyticsMetricComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsMetricComponent,
        imports: [NoopAnimationsModule],
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('value', 100);
        spectator.setInput('status', ComponentStatus.LOADED);
        spectator.setInput('animated', false);

        // Setup the mock return value
        const messageService = spectator.inject(DotMessageService);
        messageService.get.mockReturnValue('Translated Message');
    });

    describe('Component Inputs', () => {
        it('should display metric value and subtitle', () => {
            // Arrange
            spectator.setInput('value', 12345);
            spectator.setInput('subtitle', 'analytics.metrics.subtitle');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('animated', false);
            spectator.detectChanges();

            // Act & Assert
            const value = spectator.query(byTestId('metric-value'));
            const subtitle = spectator.query(byTestId('metric-subtitle'));

            expect(value).toHaveText('12,345');
            expect(subtitle).toBeTruthy();
        });

        it('should format large numbers with locale separators', () => {
            // Arrange
            spectator.setInput('value', 1234567);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('animated', false);
            spectator.detectChanges();

            // Act & Assert
            const value = spectator.query(byTestId('metric-value'));
            expect(value).toHaveText('1,234,567');
        });

        it('should display icon when provided', () => {
            // Arrange

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

            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const icon = spectator.query(byTestId('metric-icon'));
            expect(icon).toBeFalsy();
        });

        it('should not display subtitle when not provided', () => {
            // Arrange

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

            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();

            // Act & Assert
            const errorIcon = spectator.query('.pi.pi-exclamation-triangle');
            const errorMessage = spectator.query('.state-message');
            const value = spectator.query(byTestId('metric-value'));

            expect(errorIcon).toBeTruthy();
            expect(errorMessage).toBeTruthy();
            expect(value).toBeFalsy();
        });

        it('should show normal content when status is LOADED', () => {
            // Arrange

            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            const value = spectator.query(byTestId('metric-value'));
            const skeletons = spectator.queryAll('p-skeleton');
            const errorIcon = spectator.query('.pi-exclamation-triangle');

            expect(value).toBeTruthy();
            expect(skeletons.length).toBe(0);
            expect(errorIcon).toBeFalsy();
        });
    });

    describe('Computed Properties', () => {
        it('should show skeleton for INIT and LOADING, hide it for LOADED', () => {
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();
            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);

            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();
            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);

            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.queryAll('p-skeleton').length).toBe(0);
        });

        it('should show error icon for ERROR, hide it for LOADED', () => {
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();
            expect(spectator.query('.pi.pi-exclamation-triangle')).toBeTruthy();

            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.query('.pi.pi-exclamation-triangle')).toBeFalsy();
        });

        it('should apply empty class for null/undefined/empty-string values but not for 0', () => {
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.query(byTestId('metric-value'))).not.toHaveClass(
                'metric-value--empty'
            );

            spectator.setInput('value', null);
            spectator.detectChanges();
            expect(spectator.query(byTestId('metric-value'))).toHaveClass('metric-value--empty');

            spectator.setInput('value', '');
            spectator.detectChanges();
            expect(spectator.query(byTestId('metric-value'))).toHaveClass('metric-value--empty');

            spectator.setInput('value', 100);
            spectator.detectChanges();
            expect(spectator.query(byTestId('metric-value'))).not.toHaveClass(
                'metric-value--empty'
            );
        });

        it('should render icon element with correct classes when icon is provided', () => {
            spectator.setInput('value', 100);
            spectator.setInput('icon', 'pi-eye');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const icon = spectator.query(byTestId('metric-icon'));
            expect(icon).toBeTruthy();
            expect(icon).toHaveClass('pi');
            expect(icon).toHaveClass('pi-eye');
        });
    });

    describe('Empty State', () => {
        it('should NOT show empty state when value is 0 (0 is a valid metric value)', () => {
            spectator.setInput('value', 0);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('animated', false);
            spectator.detectChanges();

            const value = spectator.query(byTestId('metric-value'));

            expect(value).toExist();
            expect(value).toHaveText('0');
        });

        it('should show dash and subtitle when value is null', () => {
            spectator.setInput('value', null);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const value = spectator.query(byTestId('metric-value'));
            const subtitle = spectator.query(byTestId('metric-subtitle'));

            expect(value).toExist();
            expect(value).toHaveText('—');
            expect(value).toHaveClass('metric-value--empty');
            expect(subtitle).toExist();
        });

        it('should show dash and subtitle when value is empty string', () => {
            spectator.setInput('value', '');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const value = spectator.query(byTestId('metric-value'));
            const subtitle = spectator.query(byTestId('metric-subtitle'));

            expect(value).toExist();
            expect(value).toHaveText('—');
            expect(value).toHaveClass('metric-value--empty');
            expect(subtitle).toExist();
        });

        it('should not show empty state when value is valid', () => {
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('animated', false);
            spectator.detectChanges();

            const value = spectator.query(byTestId('metric-value'));

            expect(value).toExist();
            expect(value).not.toHaveClass('metric-value--empty');
        });
    });

    describe('Error State Updates', () => {
        it('should show updated error state with correct icon and message', () => {
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

    describe('Title Input', () => {
        it('should display title above the card when provided', () => {
            spectator.setInput('value', 100);
            spectator.setInput('title', 'analytics.metric.title');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const title = spectator.query(byTestId('metric-title'));
            expect(title).toBeTruthy();
            expect(title.tagName).toBe('H3');
        });

        it('should not display title when not provided', () => {
            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const title = spectator.query(byTestId('metric-title'));
            expect(title).toBeFalsy();
        });

        it('should render title outside the p-card element', () => {
            spectator.setInput('value', 100);
            spectator.setInput('title', 'analytics.metric.title');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            const title = spectator.query(byTestId('metric-title'));
            const card = spectator.query(byTestId('metric-card'));

            expect(title).toBeTruthy();
            expect(card).toBeTruthy();
            expect(card.contains(title)).toBe(false);
        });
    });

    describe('Accessibility', () => {
        it('should have proper test ids for testing', () => {
            // Arrange

            spectator.setInput('value', 100);
            spectator.setInput('subtitle', 'test.subtitle');
            spectator.setInput('icon', 'pi-eye');
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Act & Assert
            expect(spectator.query(byTestId('metric-card'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-value'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-subtitle'))).toBeTruthy();
            expect(spectator.query(byTestId('metric-icon'))).toBeTruthy();
        });
    });

    describe('Animations', () => {
        it('should have animation triggers configured', () => {
            // Arrange & Act

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

            spectator.setInput('value', 100);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            // Assert loading state
            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);

            // Act - Change to loaded state
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            // Assert loaded state - verify the actual content is showing
            const metricValue = spectator.query(byTestId('metric-value'));
            expect(metricValue).toBeTruthy();
            expect(metricValue).toHaveText('100');
            // Verify we're showing actual content, not skeletons
            expect(metricValue).toBeVisible();
        });
    });
});

@Component({
    selector: 'dot-test-host',
    standalone: false,
    template: `
        <dot-analytics-metric [value]="100" [status]="status">
            <div data-testid="projected-content">Projected Chart</div>
        </dot-analytics-metric>
    `
})
class TestHostComponent {
    status = ComponentStatus.LOADED;
}

describe('DotAnalyticsMetricComponent - Content Projection', () => {
    let spectator: SpectatorHost<DotAnalyticsMetricComponent, TestHostComponent>;

    const createHost = createHostFactory({
        component: DotAnalyticsMetricComponent,
        host: TestHostComponent,
        imports: [NoopAnimationsModule, DotAnalyticsMetricComponent],
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createHost(undefined, { detectChanges: false });
        const messageService = spectator.inject(DotMessageService);
        messageService.get.mockReturnValue('Translated Message');
    });

    it('should project content when provided', () => {
        spectator.hostComponent.status = ComponentStatus.LOADED;
        spectator.detectChanges();

        const projectedContent = spectator.query(byTestId('projected-content'));
        expect(projectedContent).toExist();
        expect(projectedContent).toHaveText('Projected Chart');
    });

    it('should show projected content in loading state (allows children to handle their own skeleton)', () => {
        spectator.hostComponent.status = ComponentStatus.LOADING;
        spectator.detectChanges();

        const projectedContent = spectator.query(byTestId('projected-content'));
        expect(projectedContent).toExist();
    });

    it('should hide projected content visually in error state', () => {
        spectator.hostComponent.status = ComponentStatus.ERROR;
        spectator.detectChanges();

        const container = spectator.query(byTestId('metric-projected-content'));
        expect(container).toHaveClass('metric-projected-content--hidden');
    });
});
