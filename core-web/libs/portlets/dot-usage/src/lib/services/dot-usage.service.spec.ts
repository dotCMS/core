import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
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
            imports: [HttpClientTestingModule],
            providers: [DotUsageService]
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
            expect(service.summary()).toEqual(mockSummary);
            expect(service.loading()).toBe(false);
            expect(service.error()).toBeNull();
            done();
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    it('should handle HTTP errors', (done) => {
        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (_error) => {
                expect(service.error()).toBe('usage.dashboard.error.unauthorized');
                expect(service.loading()).toBe(false);
                expect(service.summary()).toBeNull();
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle server errors', (done) => {
        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (_error) => {
                expect(service.error()).toBe('usage.dashboard.error.serverError');
                expect(service.loading()).toBe(false);
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle forbidden errors', (done) => {
        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (_error) => {
                expect(service.error()).toBe('usage.dashboard.error.forbidden');
                expect(service.loading()).toBe(false);
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });

    it('should handle network timeout errors', (done) => {
        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (_error) => {
                expect(service.error()).toBe('usage.dashboard.error.timeout');
                expect(service.loading()).toBe(false);
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush('Request Timeout', { status: 408, statusText: 'Request Timeout' });
    });

    it('should handle unknown errors', (done) => {
        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (_error) => {
                expect(service.error()).toBe('usage.dashboard.error.generic');
                expect(service.loading()).toBe(false);
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.error(new ProgressEvent('error'));
    });

    it('should handle custom error messages', (done) => {
        const customErrorMessage = 'Custom service error message';

        service.getSummary().subscribe({
            next: () => fail('Should have failed'),
            error: (_error) => {
                expect(service.error()).toBe(customErrorMessage);
                expect(service.loading()).toBe(false);
                done();
            }
        });

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush({ message: customErrorMessage }, { status: 400, statusText: 'Bad Request' });
    });

    it('should maintain loading state during request', () => {
        expect(service.loading()).toBe(false);

        service.getSummary().subscribe();
        expect(service.loading()).toBe(true);

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush({ entity: mockSummary });

        expect(service.loading()).toBe(false);
    });

    it('should clear error state when making new request', () => {
        // Set initial error state
        service.error.set('Previous error');
        expect(service.error()).toBe('Previous error');

        service.getSummary().subscribe();

        expect(service.error()).toBeNull();

        const req = httpMock.expectOne('/api/v1/usage/summary');
        req.flush({ entity: mockSummary });
    });

    it('should reset state', () => {
        // Set some state first
        service.summary.set(mockSummary);
        service.loading.set(true);
        service.error.set('Some error');

        // Reset
        service.reset();

        expect(service.summary()).toBeNull();
        expect(service.loading()).toBe(false);
        expect(service.error()).toBeNull();
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
