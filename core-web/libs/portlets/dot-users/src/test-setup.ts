import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// PrimeNG p-tabs uses ResizeObserver internally; jsdom does not
// implement it, so we install the shared mock before any test runs.
setupResizeObserverMock();
