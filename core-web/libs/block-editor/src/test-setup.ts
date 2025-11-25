// Configure Jest for Angular [https://medium.com/@kyjungok/setup-jest-in-angular-application-22b22609cbcd]
import 'jest-preset-angular/setup-jest';

import { NgModule } from '@angular/core';

// This is needed to mock the PrimeNG SplitButton component to avoid errors while running tests.
// https://github.com/primefaces/primeng/issues/12945
@NgModule()
export class SplitButtonMockModule {}

jest.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule
}));
