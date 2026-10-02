import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

import { provideZoneChangeDetection } from '@angular/core';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });
