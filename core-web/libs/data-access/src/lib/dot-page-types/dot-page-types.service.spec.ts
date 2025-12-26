import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { DotPageTypesService } from './dot-page-types.service';

const fakeResponse = {
    entity: [{ ...dotcmsContentTypeBasicMock }]
};

describe('DotPageTypesService', () => {
    let service: DotPageTypesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotPageTypesService]
        });
        service = TestBed.inject(DotPageTypesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get key', () => {
        const key = 'key1';
        expect(service).toBeTruthy();

        service.getPages(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity);
        });
        const req = httpMock.expectOne(`/api/v1/page/types?filter=${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
