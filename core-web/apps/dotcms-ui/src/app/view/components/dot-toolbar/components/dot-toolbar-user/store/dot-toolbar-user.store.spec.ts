import { TestBed } from '@angular/core/testing';

import { DotToolbarUserStore } from './dot-toolbar-user.store';

describe('DotToolbarUserService', () => {
    let service: DotToolbarUserStore;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotToolbarUserStore);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
