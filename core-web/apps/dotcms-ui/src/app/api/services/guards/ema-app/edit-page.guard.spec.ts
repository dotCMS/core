import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { editPageGuard } from './edit-page.guard';

describe('editPageGuard', () => {
    const executeGuard: CanActivateFn = (...guardParameters) =>
        TestBed.runInInjectionContext(() => editPageGuard(...guardParameters));

    beforeEach(() => {
        TestBed.configureTestingModule({});
    });

    it('should be created', () => {
        expect(executeGuard).toBeTruthy();
    });
});
