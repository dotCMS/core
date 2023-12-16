import { TestBed } from '@angular/core/testing';

import { DotActionUrlService } from './dot-action-url.service';

describe('DotActionUrlService', () => {
    let service: DotActionUrlService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotActionUrlService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
