import { TestBed } from '@angular/core/testing';

import { EmaAppGuard } from './ema-app.guard';

describe('EmaAppGuard', () => {
    let guard: EmaAppGuard;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        guard = TestBed.inject(EmaAppGuard);
    });

    it('should be created', () => {
        expect(guard).toBeTruthy();
    });
});
