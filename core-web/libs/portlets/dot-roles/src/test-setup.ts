import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv({
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
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn()
        }))
    });
}
