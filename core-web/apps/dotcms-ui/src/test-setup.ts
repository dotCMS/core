/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-empty-function */

// This file is required by jest and is used for setup for each test file.
import '@testing-library/jest-dom';
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv();

// Setup global mocks
setupResizeObserverMock();

// Add structuredClone polyfill for Jest environment
if (!global.structuredClone) {
    global.structuredClone = (obj: any) => {
        return JSON.parse(JSON.stringify(obj));
    };
}

// Add Date polyfill for Jest environment
if (!global.Date) {
    global.Date = Date;
}

// Add scrollIntoView polyfill for JSDOM
if (typeof Element !== 'undefined' && !Element.prototype.scrollIntoView) {
    Element.prototype.scrollIntoView = jest.fn();
}

// Add element.animate polyfill for Jest/JSDOM environment
if (typeof Element !== 'undefined' && !Element.prototype.animate) {
    Element.prototype.animate = function () {
        return {
            cancel: () => {},
            finish: () => {},
            play: () => {},
            pause: () => {},
            reverse: () => {},
            addEventListener: () => {},
            removeEventListener: () => {},
            dispatchEvent: () => true,
            finished: Promise.resolve(),
            ready: Promise.resolve(),
            playState: 'finished',
            playbackRate: 1,
            startTime: 0,
            currentTime: 0,
            timeline: null,
            effect: null
        };
    };
}

// Suppress JSDOM CSS parsing errors (common with PrimeNG @layer CSS)
const originalConsoleError = console.error;
console.error = (...args: unknown[]) => {
    const firstArg = typeof args[0] === 'string' ? args[0] : '';

    // Skip CSS parsing errors from JSDOM
    if (
        firstArg.includes('Error: Could not parse CSS stylesheet') ||
        (args[0]?.message && args[0].message.includes('Could not parse CSS stylesheet'))
    ) {
        return;
    }

    originalConsoleError(...args);
};

// Mock sessionStorage for JSDOM
const mockSessionStorage = {
    getItem: jest.fn().mockReturnValue(null),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
    length: 0,
    key: jest.fn()
};

Object.defineProperty(window, 'sessionStorage', {
    value: mockSessionStorage,
    writable: true
});
