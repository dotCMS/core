import { TestBed } from '@angular/core/testing';
import { DotPropertiesService } from './dot-properties.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';

const fakeResponse = {
    entity: {
        key1: 'data',
        list: ['1', '2']
    }
};

describe('DotPropertiesService', () => {
    let service: DotPropertiesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPropertiesService
            ]
        });
        service = TestBed.inject(DotPropertiesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get key', () => {
        const key = 'key1';
        expect(service).toBeTruthy();

        service.getKey(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity.key1);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    it('should get ky as a list', () => {
        const key = 'list';
        expect(service).toBeTruthy();

        service.getKeyAsList(key).subscribe((response) => {
            expect(response).toEqual(fakeResponse.entity.list);
        });
        const req = httpMock.expectOne(`/api/v1/configuration/config?keys=list:${key}`);
        expect(req.request.method).toBe('GET');
        req.flush(fakeResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
