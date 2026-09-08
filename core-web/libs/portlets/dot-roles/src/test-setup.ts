import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupTestBed({ zoneless: false });

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
