import { TestBed } from '@angular/core/testing';

import { DotEmaDialogStoreService } from './dot-ema-dialog.store';

describe('DotEmaDialogStoreService', () => {
    let service: DotEmaDialogStoreService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotEmaDialogStoreService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
