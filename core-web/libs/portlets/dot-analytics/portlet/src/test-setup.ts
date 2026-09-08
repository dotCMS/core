import '@analogjs/vitest-angular/setup-zone';

import { vi } from 'vitest';

import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';

// Analog's setup-zone patches Vitest for zone.js. A hand-rolled
// `import 'zone.js/testing'` is not enough: zone.js patches jasmine/mocha/jest,
// knows nothing about Vitest, and every fakeAsync test then fails with
// "Expected to be running in 'ProxyZone'".
getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting(), {
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// Browser API mocks required for PrimeNG 21 and Angular 21
global.MutationObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    disconnect: vi.fn(),
    takeRecords: vi.fn().mockReturnValue([])
}));

global.IntersectionObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
    takeRecords: vi.fn().mockReturnValue([])
}));

global.ResizeObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn()
}));

Element.prototype.scrollIntoView = vi.fn();
