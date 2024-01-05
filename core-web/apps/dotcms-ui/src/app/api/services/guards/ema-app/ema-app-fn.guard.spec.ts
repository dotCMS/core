import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { emaAppFnGuard } from './ema-app-fn.guard';

describe('emaAppFnGuard', () => {
    const executeGuard: CanActivateFn = (...guardParameters) =>
        TestBed.runInInjectionContext(() => emaAppFnGuard(...guardParameters));

    beforeEach(() => {
        TestBed.configureTestingModule({});
    });

    it('should be created', () => {
        expect(executeGuard).toBeTruthy();
    });
});
