/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    calculateElementVisibilityRatio,
    calculateViewportOffset,
    getViewportMetrics,
    isElementMeetingVisibilityThreshold
} from './dot-analytics.impression.utils';

describe('Impression Tracking Utils', () => {
    // Helper function to create mock element with getBoundingClientRect
    const createMockElement = (rect: Partial<DOMRect>, dataset: Record<string, string> = {}) => {
        const element = {
            getBoundingClientRect: jest.fn(() => ({
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

    describe('calculateElementVisibilityRatio', () => {
        it('should return 1.0 for fully visible element', () => {
            // Element fully visible in 1000x1000 viewport
            const element = createMockElement({
                top: 100,
                left: 100,
                bottom: 300,
                right: 300,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            expect(ratio).toBe(1);
        });

        it('should return 0.5 for element half visible from bottom', () => {
            // Element: 200x200, top at 900 (100px visible from bottom)
            const element = createMockElement({
                top: 900,
                left: 100,
                bottom: 1100, // Extends beyond viewport
                right: 300,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            // Visible area: 100px height * 200px width = 20,000
            // Total area: 200px * 200px = 40,000
            // Ratio: 20,000 / 40,000 = 0.5
            expect(ratio).toBe(0.5);
        });

        it('should return 0.5 for element half visible from top', () => {
            // Element extends above viewport
            const element = createMockElement({
                top: -100, // Starts above viewport
                left: 100,
                bottom: 100,
                right: 300,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            // Visible height: 100px (from 0 to 100)
            // Total area: 200 * 200 = 40,000
            // Visible area: 100 * 200 = 20,000
            expect(ratio).toBe(0.5);
        });

        it('should return 0 for element completely above viewport', () => {
            const element = createMockElement({
                top: -300,
                left: 100,
                bottom: -100,
                right: 300,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            expect(ratio).toBe(0);
        });

        it('should return 0 for element completely below viewport', () => {
            const element = createMockElement({
                top: 1100,
                left: 100,
                bottom: 1300,
                right: 300,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            expect(ratio).toBe(0);
        });

        it('should return 0 for element completely to the left of viewport', () => {
            const element = createMockElement({
                top: 100,
                left: -300,
                bottom: 300,
                right: -100,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            expect(ratio).toBe(0);
        });

        it('should return 0 for element completely to the right of viewport', () => {
            const element = createMockElement({
                top: 100,
                left: 1100,
                bottom: 300,
                right: 1300,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            expect(ratio).toBe(0);
        });

        it('should return 0.25 for element partially visible from corner', () => {
            // Element in bottom-right corner, 50% height and 50% width visible
            const element = createMockElement({
                top: 900,
                left: 900,
                bottom: 1100,
                right: 1100,
                width: 200,
                height: 200
            });

            const ratio = calculateElementVisibilityRatio(element);

            // Visible: 100px * 100px = 10,000
            // Total: 200px * 200px = 40,000
            // Ratio: 10,000 / 40,000 = 0.25
            expect(ratio).toBe(0.25);
        });

        it('should handle zero area elements', () => {
            const element = createMockElement({
                top: 100,
                left: 100,
                bottom: 100,
                right: 100,
                width: 0,
                height: 0
            });

            const ratio = calculateElementVisibilityRatio(element);

            expect(ratio).toBe(0);
        });

        it('should handle very large elements (larger than viewport)', () => {
            // Element larger than viewport but fully covering it
            const element = createMockElement({
                top: -500,
                left: -500,
                bottom: 1500,
                right: 1500,
                width: 2000,
                height: 2000
            });

            const ratio = calculateElementVisibilityRatio(element);

            // Visible area: 1000 * 1000 = 1,000,000
            // Total area: 2000 * 2000 = 4,000,000
            // Ratio: 1,000,000 / 4,000,000 = 0.25
            expect(ratio).toBe(0.25);
        });
    });

    describe('calculateViewportOffset', () => {
        it('should return 0% for element at viewport top', () => {
            const element = createMockElement({
                top: 0,
                bottom: 200,
                height: 200
            });

            const offset = calculateViewportOffset(element);

            expect(offset).toBe(0);
        });

        it('should return 50% for element at viewport middle', () => {
            const element = createMockElement({
                top: 500,
                bottom: 700,
                height: 200
            });

            const offset = calculateViewportOffset(element);

            // 500 / 1000 * 100 = 50%
            expect(offset).toBe(50);
        });

        it('should return 100% for element at viewport bottom', () => {
            const element = createMockElement({
                top: 1000,
                bottom: 1200,
                height: 200
            });

            const offset = calculateViewportOffset(element);

            expect(offset).toBe(100);
        });

        it('should return negative value for element above viewport', () => {
            const element = createMockElement({
                top: -200,
                bottom: 0,
                height: 200
            });

            const offset = calculateViewportOffset(element);

            // -200 / 1000 * 100 = -20%
            expect(offset).toBe(-20);
        });

        it('should round to 2 decimal places', () => {
            const element = createMockElement({
                top: 333.333,
                bottom: 533.333,
                height: 200
            });

            const offset = calculateViewportOffset(element);

            // 333.333 / 1000 * 100 = 33.3333, rounded to 33.33
            expect(offset).toBe(33.33);
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

    describe('getViewportMetrics', () => {
        it('should return both offset percentage and visibility ratio', () => {
            const element = createMockElement({
                top: 500,
                left: 100,
                bottom: 700,
                right: 300,
                width: 200,
                height: 200
            });

            const metrics = getViewportMetrics(element);

            expect(metrics).toEqual({
                offsetPercentage: 50, // top 500 / viewport 1000 * 100
                visibilityRatio: 1 // Fully visible
            });
        });

        it('should calculate metrics for partially visible element', () => {
            const element = createMockElement({
                top: 900,
                left: 900,
                bottom: 1100,
                right: 1100,
                width: 200,
                height: 200
            });

            const metrics = getViewportMetrics(element);

            expect(metrics).toEqual({
                offsetPercentage: 90,
                visibilityRatio: 0.25 // 25% visible
            });
        });
    });
});
