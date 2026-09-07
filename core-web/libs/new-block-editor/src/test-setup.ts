import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv({
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

Element.prototype.scrollIntoView = jest.fn();
