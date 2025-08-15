import { afterEach, beforeEach, describe, expect, it } from '@jest/globals';

import { isInsideUVE } from './uve.utils';

describe('UVE Utils (Internal)', () => {
    let originalWindow: Window;

    beforeEach(() => {
        originalWindow = global.window;
    });

    afterEach(() => {
        Object.defineProperty(global, 'window', {
            value: originalWindow,
            writable: true,
            configurable: true
        });
    });

    describe('isInsideUVE', () => {
        it('should return false when not in editor (parent === window)', () => {
            const originalParent = window.parent;

            // Mock window.parent to be the same as window
            Object.defineProperty(window, 'parent', {
                value: window,
                writable: true,
                configurable: true
            });

            expect(isInsideUVE()).toBe(false);

            // Restore original values
            Object.defineProperty(window, 'parent', {
                value: originalParent,
                writable: true,
                configurable: true
            });
        });

        it('should return false when window is undefined', () => {
            Object.defineProperty(global, 'window', {
                value: undefined,
                writable: true,
                configurable: true
            });

            expect(isInsideUVE()).toBe(false);
        });

        it('should return true when in editor (parent !== window)', () => {
            const mockWindow = {
                ...window,
                parent: { ...window } // Different object = in editor
            };

            Object.defineProperty(global, 'window', {
                value: mockWindow,
                writable: true,
                configurable: true
            });

            expect(isInsideUVE()).toBe(true);
        });

        it('should return true when in editor with any mode', () => {
            const mockWindow = {
                ...window,
                parent: { ...window } // Different object = in editor
            };

            Object.defineProperty(global, 'window', {
                value: mockWindow,
                writable: true,
                configurable: true
            });

            expect(isInsideUVE()).toBe(true);
        });

        it('should return true when in editor without mode parameter', () => {
            const mockWindow = {
                ...window,
                parent: { ...window } // Different object = in editor
            };

            Object.defineProperty(global, 'window', {
                value: mockWindow,
                writable: true,
                configurable: true
            });

            expect(isInsideUVE()).toBe(true);
        });
    });
});
