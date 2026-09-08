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

setupResizeObserverMock();

const originalConsoleError = console.error;
const jsDomCssError = 'Error: Could not parse CSS stylesheet';
console.error = (...params) => {
    if (params.find((p) => p?.toString()?.includes(jsDomCssError))) {
        return;
    }

    const hasXmlHttpRequestError = params.some((p) => {
        if (p && typeof p === 'object') {
            const errorObj = p as { type?: string; message?: string; name?: string };
            if (errorObj.type === 'XMLHttpRequest' || errorObj.name === 'AggregateError') {
                return true;
            }
            if (
                errorObj.message?.includes('XMLHttpRequest') ||
                (p as Error).stack?.includes('XMLHttpRequest')
            ) {
                return true;
            }
        }
        const str = p?.toString() || '';
        return str.includes('AggregateError') && str.includes('XMLHttpRequest');
    });

    if (hasXmlHttpRequestError) {
        return;
    }

    originalConsoleError(...params);
};

Element.prototype.scrollIntoView = vi.fn();
