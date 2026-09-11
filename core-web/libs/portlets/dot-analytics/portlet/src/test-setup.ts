import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { provideZoneChangeDetection } from '@angular/core';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

// Browser API mocks required for PrimeNG 21 and Angular 21
global.MutationObserver = vi.fn().mockImplementation(function () {
    return {
        observe: vi.fn(),
        disconnect: vi.fn(),
        takeRecords: vi.fn().mockReturnValue([])
    };
});

global.IntersectionObserver = vi.fn().mockImplementation(function () {
    return {
        observe: vi.fn(),
        unobserve: vi.fn(),
        disconnect: vi.fn(),
        takeRecords: vi.fn().mockReturnValue([])
    };
});

global.ResizeObserver = vi.fn().mockImplementation(function () {
    return {
        observe: vi.fn(),
        unobserve: vi.fn(),
        disconnect: vi.fn()
    };
});

Element.prototype.scrollIntoView = vi.fn();
