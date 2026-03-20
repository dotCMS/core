import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCrudService } from './dot-crud.service';

describe('DotCrudService', () => {
    let service: DotCrudService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotCrudService]
        });
        service = TestBed.inject(DotCrudService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should post data', () => {
        const mockData = { name: 'test' };
        const mockResponse = { id: '123', name: 'test' };

        service.postData('/api/v1/test', mockData).subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/v1/test');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockData);
        req.flush({ entity: mockResponse });
    });

    it('should put data', () => {
        const mockData = { name: 'updated' };
        const mockResponse = { id: '123', name: 'updated' };

        service.putData('/api/v1/test', mockData).subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/v1/test');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(mockData);
        req.flush({ entity: mockResponse });
    });

    it('should get data by id', () => {
        const mockResponse = { id: '123', name: 'test' };

        service.getDataById('/api/v1/test', '123').subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/v1/test/id/123');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockResponse });
    });

    it('should get data by id with custom pick key', () => {
        const mockContentlets = [{ id: '456', title: 'image.png' }];

        service.getDataById('/api/content', '456', 'contentlets').subscribe((response) => {
            expect(response).toEqual(mockContentlets);
        });

        const req = httpMock.expectOne('/api/content/id/456');
        expect(req.request.method).toBe('GET');
        req.flush({ contentlets: mockContentlets });
    });

    it('should delete data', () => {
        const mockResponse = { success: true };

        service.delete('/api/v1/test', '123').subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/v1/test/123');
        expect(req.request.method).toBe('DELETE');
        req.flush({ entity: mockResponse });
    });

    describe('URL normalization', () => {
        it('should preserve absolute URLs starting with /api/', () => {
            service.postData('/api/v1/contenttype', {}).subscribe();
            const req = httpMock.expectOne('/api/v1/contenttype');
            expect(req.request.url).toBe('/api/v1/contenttype');
            req.flush({ entity: {} });
        });

        it('should preserve absolute URLs starting with /', () => {
            service.postData('/custom/endpoint', {}).subscribe();
            const req = httpMock.expectOne('/custom/endpoint');
            expect(req.request.url).toBe('/custom/endpoint');
            req.flush({ entity: {} });
        });

        it('should prepend /api/ to relative v1/ paths', () => {
            service.postData('v1/contenttype', {}).subscribe();
            const req = httpMock.expectOne('/api/v1/contenttype');
            expect(req.request.url).toBe('/api/v1/contenttype');
            req.flush({ entity: {} });
        });

        it('should prepend /api/ to relative v2/ paths', () => {
            service.postData('v2/contenttype', {}).subscribe();
            const req = httpMock.expectOne('/api/v2/contenttype');
            expect(req.request.url).toBe('/api/v2/contenttype');
            req.flush({ entity: {} });
        });

        it('should prepend /api/ to relative v3/ paths', () => {
            service.postData('v3/contenttype', {}).subscribe();
            const req = httpMock.expectOne('/api/v3/contenttype');
            expect(req.request.url).toBe('/api/v3/contenttype');
            req.flush({ entity: {} });
        });

        it('should prepend /api/ to any other relative path', () => {
            service.postData('content/foo', {}).subscribe();
            const req = httpMock.expectOne('/api/content/foo');
            expect(req.request.url).toBe('/api/content/foo');
            req.flush({ entity: {} });
        });

        it('should normalize URLs in getDataById', () => {
            service.getDataById('v1/contenttype', '123').subscribe();
            const req = httpMock.expectOne('/api/v1/contenttype/id/123');
            expect(req.request.url).toBe('/api/v1/contenttype/id/123');
            req.flush({ entity: {} });
        });

        it('should normalize URLs in putData', () => {
            service.putData('v1/contenttype/id/abc', { name: 'test' }).subscribe();
            const req = httpMock.expectOne('/api/v1/contenttype/id/abc');
            expect(req.request.url).toBe('/api/v1/contenttype/id/abc');
            req.flush({ entity: {} });
        });

        it('should normalize URLs in delete', () => {
            service.delete('v1/contenttype/id', '456').subscribe();
            const req = httpMock.expectOne('/api/v1/contenttype/id/456');
            expect(req.request.url).toBe('/api/v1/contenttype/id/456');
            req.flush({ entity: {} });
        });
    });
});
