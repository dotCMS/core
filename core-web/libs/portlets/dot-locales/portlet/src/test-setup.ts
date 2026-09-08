import { vi } from 'vitest';
import '@analogjs/vitest-angular/setup-zone';

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

import { SplitButtonMockComponent, SplitButtonMockModule } from '@dotcms/utils-testing';

/*
 * This is a workaround for the following PrimeNg issue: https://github.com/primefaces/primeng/issues/12945
 * They already fixed it, but it's not in the latest v15 LTS yet: https://github.com/primefaces/primeng/pull/13597
 */
vi.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule,
    SplitButton: SplitButtonMockComponent
}));
