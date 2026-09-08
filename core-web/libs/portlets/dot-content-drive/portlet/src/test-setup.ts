import '@analogjs/vitest-angular/setup-zone';

import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

// Replaces jest-preset-angular's `getTestBed().initTestEnvironment`. Nothing initialises the TestBed
// for us here, so this file does it — and the two strictness flags are carried across
// deliberately: they turn an unknown element or property into a failure rather than a
// silent no-op, so dropping them would quietly weaken every spec in the project.
getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting(), {
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// Polyfill structuredClone (the DOM environment does not provide it)
globalThis.structuredClone ??= <T>(obj: T): T => JSON.parse(JSON.stringify(obj));

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

// Mock MutationObserver for PrimeNG components
global.MutationObserver = class MutationObserver {
    constructor() {
        //mock constructor
    }
    disconnect() {
        //mock disconnect
    }
    observe() {
        //mock observe
    }
    takeRecords(): MutationRecord[] {
        return [];
    }
};
