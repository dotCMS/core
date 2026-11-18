import { vi } from 'vitest';

import { isElementMeetingVisibilityThreshold } from './utils';

describe('Impression Tracking Utils', () => {
    // Helper function to create mock element with getBoundingClientRect
    const createMockElement = (rect: Partial<DOMRect>, dataset: Record<string, string> = {}) => {
        const element = {
            getBoundingClientRect: vi.fn(() => ({
                top: 0,
                left: 0,
                bottom: 0,
                right: 0,
                width: 0,
                height: 0,
                x: 0,
                y: 0,
                toJSON: () => ({}),
                ...rect
            })),
            dataset
        } as unknown as HTMLElement;

        return element;
    };

    // Save original values
    let originalInnerHeight: number;
    let originalInnerWidth: number;

    beforeEach(() => {
        // Save originals
        originalInnerHeight = window.innerHeight;
        originalInnerWidth = window.innerWidth;

        // Set default viewport size
        Object.defineProperty(window, 'innerHeight', { value: 1000, writable: true });
        Object.defineProperty(window, 'innerWidth', { value: 1000, writable: true });

        // Mock document.documentElement if needed
        Object.defineProperty(document.documentElement, 'clientHeight', {
            value: 1000,
            writable: true,
            configurable: true
        });
        Object.defineProperty(document.documentElement, 'clientWidth', {
            value: 1000,
            writable: true,
            configurable: true
        });
    });

    afterEach(() => {
        // Restore originals
        Object.defineProperty(window, 'innerHeight', {
            value: originalInnerHeight,
            writable: true
        });
        Object.defineProperty(window, 'innerWidth', {
            value: originalInnerWidth,
            writable: true
        });
    });

    describe('isElementMeetingVisibilityThreshold', () => {
        it('should return true when visibility meets threshold exactly', () => {
            const element = createMockElement({
                top: 500,
                left: 100,
                bottom: 700,
                right: 300,
                width: 200,
                height: 200
            });

            const result = isElementMeetingVisibilityThreshold(element, 1.0);

            expect(result).toBe(true);
        });

        it('should return true when visibility exceeds threshold', () => {
            const element = createMockElement({
                top: 100,
                left: 100,
                bottom: 300,
                right: 300,
                width: 200,
                height: 200
            });

            const result = isElementMeetingVisibilityThreshold(element, 0.5);

            expect(result).toBe(true);
        });

        it('should return false when visibility is below threshold', () => {
            const element = createMockElement({
                top: 900,
                left: 100,
                bottom: 1100,
                right: 300,
                width: 200,
                height: 200
            });

            // Visibility ratio is 0.5, threshold is 0.6
            const result = isElementMeetingVisibilityThreshold(element, 0.6);

            expect(result).toBe(false);
        });

        it('should return false for completely hidden element', () => {
            const element = createMockElement({
                top: 1100,
                left: 100,
                bottom: 1300,
                right: 300,
                width: 200,
                height: 200
            });

            const result = isElementMeetingVisibilityThreshold(element, 0.1);

            expect(result).toBe(false);
        });
    });
});
