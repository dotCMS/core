import { TestBed } from '@angular/core/testing';

import { DotTemplateListResolver } from './dot-template-list-resolver.service';
import { PaginatorService } from '@services/paginator';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

describe('DotTemplateListResolverService', () => {
    let service: DotTemplateListResolver;
    let paginationService: PaginatorService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [PaginatorService, { provide: CoreWebService, useClass: CoreWebServiceMock }]
        });
        service = TestBed.inject(DotTemplateListResolver);
        paginationService = TestBed.inject(PaginatorService);
    });

    it('should sent endpoint and get first page', () => {
        spyOn(paginationService, 'getFirstPage').and.returnValue(of([]));

        service.resolve();
        expect(paginationService.url).toEqual('/api/v1/templates');
        expect(paginationService.getFirstPage).toHaveBeenCalled();
    });
});
