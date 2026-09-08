import { vi } from 'vitest';
import '@analogjs/vitest-angular/setup-zone';

import { NgModule } from '@angular/core';
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

// This is needed to mock the PrimeNG SplitButton component to avoid errors while running tests.
// https://github.com/primefaces/primeng/issues/12945
@NgModule()
export class SplitButtonMockModule {}

vi.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule
}));
