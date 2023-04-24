import { TestBed } from '@angular/core/testing';

import { DotCopyContentModalService } from './dot-copy-content-modal.service';

describe('DotCopyContentModalService', () => {
    let service: DotCopyContentModalService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotCopyContentModalService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
