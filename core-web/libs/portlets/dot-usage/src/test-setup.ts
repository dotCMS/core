import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

import { provideZoneChangeDetection } from '@angular/core';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

const originalConsoleError = console.error;

// To avoid the error: Error: Could not parse CSS stylesheet
// https://stackoverflow.com/questions/69906136/console-error-error-could-not-parse-css-stylesheet
console.error = function (...data) {
    if (
        typeof data[0]?.toString === 'function' &&
        data[0].toString().includes('Error: Could not parse CSS stylesheet')
    )
        return;
    originalConsoleError(...data);
};

// We need to setup the ResizeObserver mock globally for testing
// PrimeNG Tabs use this and fails if not setup
setupResizeObserverMock();
