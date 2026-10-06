/* eslint-disable @typescript-eslint/no-explicit-any */
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

import { provideZoneChangeDetection } from '@angular/core';
import '@analogjs/vitest-angular/setup-zone';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

if (!global.structuredClone) {
    global.structuredClone = (obj: any) => JSON.parse(JSON.stringify(obj));
}
