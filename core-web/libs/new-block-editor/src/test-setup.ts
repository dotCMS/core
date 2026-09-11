import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { provideZoneChangeDetection } from '@angular/core';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

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
