import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

import { NgModule } from '@angular/core';

// This is needed to mock the PrimeNG SplitButton component to avoid errors while running tests.
// https://github.com/primefaces/primeng/issues/12945
@NgModule()
export class SplitButtonMockModule {}

jest.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule
}));
