import { of } from 'rxjs';

import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { serverErrorInterceptor } from './server-error.interceptor';

describe('serverErrorInterceptor', () => {
    let httpClient: HttpClient;
    let httpMock: HttpTestingController;
    let handleSpy: jest.Mock;

    beforeEach(() => {
        handleSpy = jest.fn().mockReturnValue(of({ redirected: false, status: 500 }));

        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withInterceptors([serverErrorInterceptor])),
                provideHttpClientTesting(),
                {
                    provide: DotHttpErrorManagerService,
                    useValue: { handle: handleSpy }
                }
            ]
        });

        httpClient = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should pass successful requests through without calling error handler', () => {
        let result: unknown;
        httpClient.get('/api/test').subscribe((data) => {
            result = data;
        });

        httpMock.expectOne('/api/test').flush({ entity: 'ok' });

        expect(result).toEqual({ entity: 'ok' });
        expect(handleSpy).not.toHaveBeenCalled();
    });

    it('should route 500 errors through DotHttpErrorManagerService', () => {
        let errorReceived = false;

        httpClient.get('/api/test').subscribe({
            next: () => fail('should not emit next'),
            error: () => {
                errorReceived = true;
            }
        });

        httpMock
            .expectOne('/api/test')
            .flush(
                { message: 'Server Error' },
                { status: 500, statusText: 'Internal Server Error' }
            );

        expect(handleSpy).toHaveBeenCalledWith(expect.objectContaining({ status: 500 }));
        expect(errorReceived).toBe(true);
    });

    it('should route 401 errors through DotHttpErrorManagerService', () => {
        handleSpy.mockReturnValue(of({ redirected: true, status: 401 }));

        let errorReceived = false;

        httpClient.get('/api/test').subscribe({
            next: () => fail('should not emit next'),
            error: () => {
                errorReceived = true;
            }
        });

        httpMock
            .expectOne('/api/test')
            .flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

        expect(handleSpy).toHaveBeenCalledWith(expect.objectContaining({ status: 401 }));
        expect(errorReceived).toBe(true);
    });

    it('should route 403 errors through DotHttpErrorManagerService', () => {
        handleSpy.mockReturnValue(of({ redirected: false, status: 403 }));

        let errorReceived = false;

        httpClient.get('/api/test').subscribe({
            next: () => fail('should not emit next'),
            error: () => {
                errorReceived = true;
            }
        });

        httpMock
            .expectOne('/api/test')
            .flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

        expect(handleSpy).toHaveBeenCalledWith(expect.objectContaining({ status: 403 }));
        expect(errorReceived).toBe(true);
    });

    it('should route 404 errors through DotHttpErrorManagerService', () => {
        handleSpy.mockReturnValue(of({ redirected: false, status: 404 }));

        let errorReceived = false;

        httpClient.get('/api/test').subscribe({
            next: () => fail('should not emit next'),
            error: () => {
                errorReceived = true;
            }
        });

        httpMock
            .expectOne('/api/test')
            .flush({ message: 'Not Found' }, { status: 404, statusText: 'Not Found' });

        expect(handleSpy).toHaveBeenCalledWith(expect.objectContaining({ status: 404 }));
        expect(errorReceived).toBe(true);
    });
});
