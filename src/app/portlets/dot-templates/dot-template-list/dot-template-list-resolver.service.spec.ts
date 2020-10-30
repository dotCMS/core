import { TestBed } from '@angular/core/testing';

import { DotTemplateListResolver } from './dot-template-list-resolver.service';

xdescribe('DotTemplateListResolverService', () => {
    let service: DotTemplateListResolver;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotTemplateListResolver);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
