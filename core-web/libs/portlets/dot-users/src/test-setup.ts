import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

import { setupResizeObserverMock } from '@dotcms/utils-testing';

setupTestBed({ zoneless: false });

// PrimeNG p-tabs uses ResizeObserver internally; jsdom does not
// implement it, so we install the shared mock before any test runs.
setupResizeObserverMock();
