import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

import { NgModule } from '@angular/core';

setupTestBed({ zoneless: false });

// This is needed to mock the PrimeNG SplitButton component to avoid errors while running tests.
// https://github.com/primefaces/primeng/issues/12945
@NgModule()
export class SplitButtonMockModule {}

vi.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule
}));
