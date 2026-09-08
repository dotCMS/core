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

// Workaround for the following issue:
// https://github.com/jsdom/jsdom/issues/2177#issuecomment-1724971596
const originalConsoleError = console.error;
const jsDomCssError = 'Error: Could not parse CSS stylesheet';
console.error = (...params) => {
    if (!params.find((p) => p.toString().includes(jsDomCssError))) {
        originalConsoleError(...params);
    }
};
