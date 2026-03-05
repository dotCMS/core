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
const jsDomNavigationError = 'Not implemented: navigation';
console.error = (...params) => {
    const msg =
        params
            .find((p) => typeof p === 'string' || (p && typeof (p as Error).message === 'string'))
            ?.toString?.() ?? '';
    if (msg.includes(jsDomCssError) || msg.includes(jsDomNavigationError)) {
        return;
    }
    originalConsoleError(...params);
};

// Filter all console warnings during tests
console.warn = () => {
    // do nothing so it doesn't print warnings that are not relevant to the tests
};

// JSDOM does not implement navigation (location.reload/assign/replace throw "Not implemented: navigation").
// Patch Location.prototype so all location objects (including iframe contentWindow.location) use no-ops in tests.
if (typeof window !== 'undefined' && window.location?.constructor?.prototype) {
    const noop = jest.fn();
    const proto = window.location.constructor.prototype as Record<string, unknown>;
    for (const method of ['reload', 'assign', 'replace']) {
        if (proto[method] !== noop) {
            Object.defineProperty(proto, method, {
                value: noop,
                configurable: true,
                writable: true,
                enumerable: true
            });
        }
    }
}
