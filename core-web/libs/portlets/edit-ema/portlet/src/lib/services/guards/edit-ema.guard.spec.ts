import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { editEmaGuard } from './edit-ema.guard';

describe('editEmaGuard', () => {
    const executeGuard: CanActivateFn = (...guardParameters) =>
        TestBed.runInInjectionContext(() => editEmaGuard(...guardParameters));

    beforeEach(() => {
        TestBed.configureTestingModule({});
    });

    it('should be created', () => {
        expect(executeGuard).toBeTruthy();
    });
});
