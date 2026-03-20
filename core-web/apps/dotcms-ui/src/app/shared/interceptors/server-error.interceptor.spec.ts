import { of } from 'rxjs';

import {
    HttpClient,
    HttpErrorResponse,
    provideHttpClient,
    withInterceptors
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { serverErrorInterceptor } from './server-error.interceptor';

describe('serverErrorInterceptor', () => {
    let httpClient: HttpClient;
    let httpMock: HttpTestingController;
    let errorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withInterceptors([serverErrorInterceptor])),
                provideHttpClientTesting(),
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jest.fn().mockReturnValue(of({ redirected: false, status: 500 }))
                    }
                }
            ]
        });

        httpClient = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
        errorManagerService = TestBed.inject(DotHttpErrorManagerService);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should pass successful requests through', (done) => {
        httpClient.get('/api/test').subscribe((data) => {
            expect(data).toEqual({ entity: 'ok' });
            done();
        });

        httpMock.expectOne('/api/test').flush({ entity: 'ok' });
    });

    it('should route errors through DotHttpErrorManagerService and re-throw', (done) => {
        httpClient.get('/api/test').subscribe({
            error: (error: HttpErrorResponse) => {
                expect(errorManagerService.handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 500 })
                );
                expect(error.status).toBe(500);
                done();
            }
        });

        httpMock.expectOne('/api/test').flush('Server Error', {
            status: 500,
            statusText: 'Internal Server Error'
        });
    });

    it('should handle 401 errors', (done) => {
        (errorManagerService.handle as jest.Mock).mockReturnValue(
            of({ redirected: true, status: 401 })
        );

        httpClient.get('/api/test').subscribe({
            error: (error: HttpErrorResponse) => {
                expect(errorManagerService.handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 401 })
                );
                expect(error.status).toBe(401);
                done();
            }
        });

        httpMock.expectOne('/api/test').flush('Unauthorized', {
            status: 401,
            statusText: 'Unauthorized'
        });
    });

    it('should handle 403 errors', (done) => {
        (errorManagerService.handle as jest.Mock).mockReturnValue(
            of({ redirected: false, status: 403 })
        );

        httpClient.get('/api/test').subscribe({
            error: (error: HttpErrorResponse) => {
                expect(errorManagerService.handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 403 })
                );
                expect(error.status).toBe(403);
                done();
            }
        });

        httpMock.expectOne('/api/test').flush('Forbidden', {
            status: 403,
            statusText: 'Forbidden'
        });
    });

    it('should handle 404 errors', (done) => {
        (errorManagerService.handle as jest.Mock).mockReturnValue(
            of({ redirected: false, status: 404 })
        );

        httpClient.get('/api/test').subscribe({
            error: (error: HttpErrorResponse) => {
                expect(errorManagerService.handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 404 })
                );
                expect(error.status).toBe(404);
                done();
            }
        });

        httpMock.expectOne('/api/test').flush('Not Found', {
            status: 404,
            statusText: 'Not Found'
        });
    });
});
