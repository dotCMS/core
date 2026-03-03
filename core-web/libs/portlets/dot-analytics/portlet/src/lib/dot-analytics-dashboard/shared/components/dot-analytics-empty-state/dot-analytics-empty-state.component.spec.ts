import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotAnalyticsEmptyStateComponent } from './dot-analytics-empty-state.component';

describe('DotAnalyticsEmptyStateComponent', () => {
    let spectator: Spectator<DotAnalyticsEmptyStateComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsEmptyStateComponent,
        providers: [mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })]
    });

    it('should create', () => {
        spectator = createComponent();
        expect(spectator.component).toBeTruthy();
    });

    it('should use default message and icon', () => {
        spectator = createComponent();

        const icon = spectator.query('i');
        expect(icon).toHaveClass('pi-info-circle');

        const message = spectator.query('p');
        expect(message).toContainText('analytics.charts.empty.description');
    });

    it('should accept custom message', () => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('message', 'custom.message.key');
        spectator.detectChanges();

        const message = spectator.query('p');
        expect(message).toContainText('custom.message.key');
    });

    it('should accept custom icon', () => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('icon', 'pi-exclamation-triangle');
        spectator.detectChanges();

        const icon = spectator.query('i');
        expect(icon).toHaveClass('pi-exclamation-triangle');
    });

    it('should have flex container for centering', () => {
        spectator = createComponent();
        const container = spectator.query('.flex.flex-col.flex-1');
        expect(container).toExist();
    });
});
