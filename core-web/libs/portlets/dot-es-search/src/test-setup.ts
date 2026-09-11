import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { provideZoneChangeDetection } from '@angular/core';

import { monacoMock } from '@dotcms/utils-testing';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

// PrimeNG TabList requires ResizeObserver which is not available in jsdom
class MockResizeObserver {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
}

Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    configurable: true,
    value: MockResizeObserver
});

/**
 * The Monaco editor reads a GLOBAL `monaco`, loaded from an asset at runtime and absent
 * under test: `initEditor()` threw `ReferenceError: monaco is not defined` from a
 * lifecycle hook. Angular reports that asynchronously, so it surfaced only
 * intermittently — the call sits behind a filter/take, and this project went several
 * full runs green before it fired.
 */
Object.defineProperty(globalThis, 'monaco', {
    writable: true,
    configurable: true,
    value: monacoMock
});
