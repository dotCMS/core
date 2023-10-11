import { TestBed } from '@angular/core/testing';

import { DotEditContentService } from './dot-edit-content.service';

describe('DotEditContentService', () => {
    let service: DotEditContentService;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotEditContentService] });
        service = TestBed.inject(DotEditContentService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
