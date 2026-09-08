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

// PrimeNG Tabs (and other layout components) call ResizeObserver on mount.
setupResizeObserverMock();

// PrimeNG ConfirmDialog / ContextMenu read `window.matchMedia` for their
// responsive breakpoints; jsdom doesn't ship an implementation, so stub it
// with a "never matches" mock — good enough for unit tests where breakpoint
// behavior isn't asserted.
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
