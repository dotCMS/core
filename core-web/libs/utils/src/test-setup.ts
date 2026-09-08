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

// Global mocks for jsdom
const mock = () => {
    let storage: { [key: string]: string } = {};
    return {
        getItem: (key: string) => (key in storage ? storage[key] : null),
        setItem: (key: string, value: string) => (storage[key] = value || ''),
        removeItem: (key: string) => delete storage[key],
        clear: () => (storage = {})
    };
};

Object.defineProperty(window, 'localStorage', { value: mock() });
Object.defineProperty(window, 'sessionStorage', { value: mock() });
Object.defineProperty(window, 'getComputedStyle', {
    value: () => ({
        getPropertyValue: (_prop: string) => {
            return '';
        },
        setProperty: (_propertyName: string, _value: string) => {
            return;
        }
    })
});

Object.defineProperty(document.body.style, 'transform', {
    value: () => ({
        enumerable: true,
        configurable: true
    })
});
