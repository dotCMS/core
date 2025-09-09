// This file is required by jest and is used for setup for each test file.
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupZoneTestEnv();

// Setup global mocks
setupResizeObserverMock();

// Suppress JSDOM CSS parsing errors (common with PrimeNG @layer CSS)
const originalConsoleError = console.error;
console.error = (...args: unknown[]) => {
    const firstArg = typeof args[0] === 'string' ? args[0] : '';

    // Skip CSS parsing errors from JSDOM
    if (
        firstArg.includes('Error: Could not parse CSS stylesheet') ||
        (args[0]?.message && args[0].message.includes('Could not parse CSS stylesheet'))
    ) {
        return;
    }

    originalConsoleError(...args);
};
