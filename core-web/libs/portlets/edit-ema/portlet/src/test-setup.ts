import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

// 10s max per test to catch infinite loops / runaway tests
vi.setConfig({ testTimeout: 10000 });

setupTestBed({ zoneless: false });

// Setup global mocks
setupResizeObserverMock();

// JSDOM doesn't implement matchMedia. PrimeNG's TooltipModule and a few
// other Angular CDK utilities call it during init; without this stub
// they throw "window.matchMedia is not a function" before tests run.
if (typeof window !== 'undefined' && !window.matchMedia) {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: vi.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: vi.fn(),
            removeListener: vi.fn(),
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn()
        }))
    });
}

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
    const noop = vi.fn();
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
