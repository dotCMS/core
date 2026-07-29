import { alignOverlayLeftToTrigger } from './host-folder-field-overlay.utils';

describe('alignOverlayLeftToTrigger', () => {
    it('should align the overlay left edge to the trigger left edge', () => {
        const trigger = document.createElement('button');
        const container = document.createElement('div');

        jest.spyOn(trigger, 'getBoundingClientRect').mockReturnValue({
            left: 120,
            top: 0,
            right: 420,
            bottom: 40,
            width: 300,
            height: 40,
            x: 120,
            y: 0,
            toJSON: () => ({})
        });
        Object.defineProperty(container, 'offsetWidth', { value: 640 });

        alignOverlayLeftToTrigger(trigger, container);

        expect(container.style.left).toBe('120px');
    });

    it('should clamp the overlay when it would overflow the viewport', () => {
        const trigger = document.createElement('button');
        const container = document.createElement('div');

        jest.spyOn(trigger, 'getBoundingClientRect').mockReturnValue({
            left: 900,
            top: 0,
            right: 1200,
            bottom: 40,
            width: 300,
            height: 40,
            x: 900,
            y: 0,
            toJSON: () => ({})
        });
        Object.defineProperty(container, 'offsetWidth', { value: 640 });
        Object.defineProperty(window, 'innerWidth', { value: 1024, configurable: true });

        alignOverlayLeftToTrigger(trigger, container);

        expect(container.style.left).toBe('384px');
    });
});
