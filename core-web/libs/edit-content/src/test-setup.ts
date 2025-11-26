import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// Setup global mocks
setupResizeObserverMock();

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
Element.prototype.scrollIntoView = jest.fn();
