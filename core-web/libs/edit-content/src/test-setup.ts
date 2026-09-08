import '@analogjs/vitest-angular/setup-zone';

import { vi } from 'vitest';

import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

// Analog's setup-zone patches Vitest for zone.js. A hand-rolled
// `import 'zone.js/testing'` is not enough: zone.js patches jasmine/mocha/jest,
// knows nothing about Vitest, and every fakeAsync test then fails with
// "Expected to be running in 'ProxyZone'".
getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting(), {
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// Setup global mocks
setupResizeObserverMock();

// Polyfill structuredClone for Jest/Node (used by host-folder-field store tree cloning)
globalThis.structuredClone ??= <T>(obj: T): T => JSON.parse(JSON.stringify(obj)) as T;

// Workaround for the following issue:
// https://github.com/jsdom/jsdom/issues/2177#issuecomment-1724971596
const originalConsoleError = console.error;
const jsDomCssError = 'Error: Could not parse CSS stylesheet';
console.error = (...params) => {
    // Check if any parameter contains the CSS error
    if (params.find((p) => p?.toString()?.includes(jsDomCssError))) {
        return;
    }

    // Check for XMLHttpRequest AggregateError from JSDOM
    // This occurs when jsdom tries to load external resources (CSS, images, etc.)
    const hasXmlHttpRequestError = params.some((p) => {
        if (p && typeof p === 'object') {
            // Check error object properties
            const errorObj = p as { type?: string; message?: string; name?: string };
            if (errorObj.type === 'XMLHttpRequest' || errorObj.name === 'AggregateError') {
                return true;
            }
            // Check stack trace
            if (
                errorObj.message?.includes('XMLHttpRequest') ||
                (p as Error).stack?.includes('XMLHttpRequest')
            ) {
                return true;
            }
        }
        // Check string representation
        const str = p?.toString() || '';
        return str.includes('AggregateError') && str.includes('XMLHttpRequest');
    });

    if (hasXmlHttpRequestError) {
        return;
    }

    originalConsoleError(...params);
};

// Mock DOM APIs not available in JSDOM
Element.prototype.scrollIntoView = vi.fn();
