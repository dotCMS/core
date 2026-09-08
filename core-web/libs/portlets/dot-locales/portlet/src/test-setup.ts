import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
import { vi } from 'vitest';

setupTestBed({ zoneless: false });
import { SplitButtonMockComponent, SplitButtonMockModule } from '@dotcms/utils-testing';

/*
 * This is a workaround for the following PrimeNg issue: https://github.com/primefaces/primeng/issues/12945
 * They already fixed it, but it's not in the latest v15 LTS yet: https://github.com/primefaces/primeng/pull/13597
 */
vi.mock('primeng/splitbutton', () => ({
    SplitButtonModule: SplitButtonMockModule,
    SplitButton: SplitButtonMockComponent
}));
