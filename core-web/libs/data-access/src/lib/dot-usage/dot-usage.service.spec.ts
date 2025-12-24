import { provideHttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotUsageService, UsageApiResponse, UsageSummary } from './dot-usage.service';

describe('DotUsageService', () => {
    let service: DotUsageService;
    let httpMock: HttpTestingController;

    const mockSummary: UsageSummary = {
        metrics: {
            content: {
                COUNT_CONTENT: {
                    name: 'COUNT_CONTENT',
                    value: 1500,
                    displayLabel: 'usage.metric.COUNT_CONTENT'
                }
            },
            site: {
                COUNT_OF_SITES: {
                    name: 'COUNT_OF_SITES',
                    value: 5,
                    displayLabel: 'usage.metric.COUNT_OF_SITES'
                }
            },
            user: {
                COUNT_OF_USERS: {
                    name: 'COUNT_OF_USERS',
                    value: 60,
                    displayLabel: 'usage.metric.COUNT_OF_USERS'
                }
            },
            system: {
                COUNT_LANGUAGES: {
                    name: 'COUNT_LANGUAGES',
                    value: 3,
                    displayLabel: 'usage.metric.COUNT_LANGUAGES'
                }
            }
        },
        lastUpdated: '2024-01-15T15:30:00Z'
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotUsageService]
        });

        service = TestBed.inject(DotUsageService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should get summary successfully', (done) => {
        const mockResponse: UsageApiResponse = { entity: mockSummary };

        service.getSummary().subscribe((summary) => {
            expect(summary).toEqual(mockSummary);
            done();
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    it('should handle HTTP errors', (done) => {
        const errorSpy = jest.spyOn(console, 'error').mockImplementation();

        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (error) => {
                expect(error.status).toBe(401);
                errorSpy.mockRestore();
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle server errors', (done) => {
        const errorSpy = jest.spyOn(console, 'error').mockImplementation();

        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (error) => {
                expect(error.status).toBe(500);
                errorSpy.mockRestore();
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should get error message for 401', () => {
        const error = { status: 401 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.unauthorized');
    });

    it('should get error message for 403', () => {
        const error = { status: 403 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.forbidden');
    });

    it('should get error message for 404', () => {
        const error = { status: 404 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.notFound');
    });

    it('should get error message for 408', () => {
        const error = { status: 408 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.timeout');
    });

    it('should get error message for 500', () => {
        const error = { status: 500 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.serverError');
    });

    it('should get error message for 502', () => {
        const error = { status: 502 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.badGateway');
    });

    it('should get error message for 503', () => {
        const error = { status: 503 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.serviceUnavailable');
    });

    it('should get error message for unknown status', () => {
        const error = { status: 418 } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.requestFailed');
    });

    it('should get error message from error.error.message', () => {
        const error = {
            error: { message: 'Custom error message' },
            status: 400
        } as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('Custom error message');
    });

    it('should get generic error message when no status', () => {
        const error = {} as HttpErrorResponse;
        expect(service.getErrorMessage(error)).toBe('usage.dashboard.error.generic');
    });

    it('should refresh data', (done) => {
        const mockResponse: UsageApiResponse = { entity: mockSummary };

        service.refresh().subscribe((summary) => {
            expect(summary).toEqual(mockSummary);
            done();
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush(mockResponse);
    });

    it('should handle concurrent requests properly', () => {
        const spy = jest.spyOn(console, 'error').mockImplementation();

        // Start two requests simultaneously
        service.getSummary().subscribe();
        service.getSummary().subscribe();

        const requests = httpMock.match('/api/v1/usage/summary');
        expect(requests.length).toBe(2);

        // Fulfill both requests
        requests[0].flush({ entity: mockSummary });
        requests[1].flush({ entity: mockSummary });

        spy.mockRestore();
    });

    it('should validate response structure', (done) => {
        const invalidResponse = { invalidProperty: 'test' };

        service.getSummary().subscribe({
            next: (summary) => {
                // Should handle invalid response gracefully
                expect(summary).toBeDefined();
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush({ entity: invalidResponse });
    });
});

