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

    it('should delete data', () => {
        const mockResponse = { success: true };

        service.delete('/api/v1/test', '123').subscribe((response) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/v1/test/123');
        expect(req.request.method).toBe('DELETE');
        req.flush({ entity: mockResponse });
    });
});
