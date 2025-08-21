import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

import { SplitButtonMockComponent, SplitButtonMockModule } from '@dotcms/utils-testing';

/*
 * This is a workaround for the following PrimeNg issue: https://github.com/primefaces/primeng/issues/12945
 * They already fixed it, but it's not in the latest v15 LTS yet: https://github.com/primefaces/primeng/pull/13597
 */
jest.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule,
    SplitButton: SplitButtonMockComponent
}));
