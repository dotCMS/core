import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

import { provideZoneChangeDetection } from '@angular/core';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

// PrimeNG TabList requires ResizeObserver, which jsdom does not provide.
setupResizeObserverMock();
