import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// Setup global mocks
setupResizeObserverMock();

// Polyfill structuredClone for Jest/Node environment (not available in Node < 17)
globalThis.structuredClone ??= <T>(obj: T): T => JSON.parse(JSON.stringify(obj)) as T;

// Workaround for the following issue:
// https://github.com/jsdom/jsdom/issues/2177#issuecomment-1724971596
const originalConsoleError = console.error;
const jsDomCssError = 'Error: Could not parse CSS stylesheet';
console.error = (...params) => {
    if (!params.find((p) => p.toString().includes(jsDomCssError))) {
        originalConsoleError(...params);
    }
};

// Filter all console warnings during tests
console.warn = () => {
    // do nothing so it doesn't print warnings that are not relevant to the tests
};

// JSDOM does not implement navigation (location.reload throws "Not implemented: navigation").
// Patch Location.prototype so all location objects (including iframe contentWindow.location) use a no-op in tests.
if (typeof window !== 'undefined' && window.location?.constructor?.prototype) {
    Object.defineProperty(window.location.constructor.prototype, 'reload', {
        value: jest.fn(),
        configurable: true,
        writable: true,
        enumerable: true
    });
}
