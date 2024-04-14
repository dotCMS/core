import { TestBed } from '@angular/core/testing';

import { InlineEditService } from '../inline-edit.service';

describe('InlineEditService', () => {
    let service: InlineEditService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(InlineEditService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
