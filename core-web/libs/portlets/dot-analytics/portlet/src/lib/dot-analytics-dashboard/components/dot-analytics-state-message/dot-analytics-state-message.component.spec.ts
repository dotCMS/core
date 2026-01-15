import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotAnalyticsStateMessageComponent } from './dot-analytics-state-message.component';

describe('DotAnalyticsStateMessageComponent', () => {
    let spectator: Spectator<DotAnalyticsStateMessageComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsStateMessageComponent,
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                message: 'test.message',
                icon: 'pi-info-circle'
            }
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render the icon with correct classes', () => {
        const iconElement = spectator.query('i');

        expect(iconElement).toBeTruthy();
        expect(iconElement).toHaveClass('pi');
        expect(iconElement).toHaveClass('pi-info-circle');
        expect(iconElement).toHaveClass('text-gray-400');
        expect(iconElement).toHaveClass('text-4xl!');
    });

    it('should render the message element', () => {
        const messageElement = spectator.query('.text-base.text-gray-800');

        expect(messageElement).toBeTruthy();
        expect(messageElement).toBeVisible();
    });

    it('should update icon classes when inputs change', () => {
        spectator.setInput('icon', 'pi-exclamation-triangle');

        const iconElement = spectator.query('i');

        expect(iconElement).toHaveClass('pi-exclamation-triangle');
        expect(iconElement).not.toHaveClass('pi-info-circle');
    });

    it('should apply custom icon size and color', () => {
        spectator.setInput('iconSize', 'text-xl');
        spectator.setInput('iconColor', 'text-red-500');

        const iconElement = spectator.query('i');

        expect(iconElement).toHaveClass('text-xl');
        expect(iconElement).toHaveClass('text-red-500');
        expect(iconElement).not.toHaveClass('text-4xl!');
        expect(iconElement).not.toHaveClass('text-gray-400');
    });

    it('should apply additional icon classes', () => {
        spectator.setInput('iconClasses', 'custom-class');

        const iconElement = spectator.query('i');

        expect(iconElement).toHaveClass('custom-class');
    });

    it('should have correct component structure', () => {
        const container = spectator.query('.flex.flex-col.justify-center');
        const iconElement = spectator.query('i');
        const messageElement = spectator.query('.text-base.text-gray-800');

        expect(container).toBeTruthy();
        expect(iconElement).toBeTruthy();
        expect(messageElement).toBeTruthy();
    });
});
