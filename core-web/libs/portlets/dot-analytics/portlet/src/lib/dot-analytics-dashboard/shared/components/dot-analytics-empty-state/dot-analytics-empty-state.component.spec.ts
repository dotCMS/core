import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotAnalyticsEmptyStateComponent } from './dot-analytics-empty-state.component';

describe('DotAnalyticsEmptyStateComponent', () => {
    let spectator: Spectator<DotAnalyticsEmptyStateComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsEmptyStateComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: (key: string) => key
                }
            }
        ]
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
        spectator = createComponent({
            props: { message: 'custom.message.key' } as unknown
        });

        const message = spectator.query('p');
        expect(message).toContainText('custom.message.key');
    });

    it('should accept custom icon', () => {
        spectator = createComponent({
            props: { icon: 'pi-exclamation-triangle' } as unknown
        });

        const icon = spectator.query('i');
        expect(icon).toHaveClass('pi-exclamation-triangle');
    });

    it('should have flex host styles for centering', () => {
        spectator = createComponent();
        const container = spectator.query('.flex.flex-col.items-center.justify-center');
        expect(container).toExist();
    });
});
