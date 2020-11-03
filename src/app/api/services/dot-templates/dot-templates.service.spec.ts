import { TestBed } from '@angular/core/testing';

import { DotTemplatesService } from './dot-templates.service';

xdescribe('DotTemplatesService', () => {
    let service: DotTemplatesService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotTemplatesService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
