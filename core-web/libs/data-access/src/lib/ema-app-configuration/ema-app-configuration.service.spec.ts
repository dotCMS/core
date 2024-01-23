import { createHttpFactory, SpectatorService } from '@ngneat/spectator';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { EmaAppConfigurationService } from './ema-app-configuration.service';

describe('EmaAppConfigurationService', () => {
    let spectator: SpectatorService<EmaAppConfigurationService>;
    let httpTestingController: HttpTestingController;

    const createService = createHttpFactory({
        service: EmaAppConfigurationService,
        imports: [HttpClientTestingModule, RouterTestingModule],
        providers: []
    });

    beforeEach(() => {
        spectator = createService();
        httpTestingController = spectator.inject(HttpTestingController);

        jest.resetAllMocks(); // Reset all mocks before each test
    });

    describe('get', () => {
        describe('should return null', () => {
            it('if get app config return bad request', (done) => {
                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/ema');

                const mockErrorResponse = { status: 400, statusText: 'Bad Request' };
                const mockResponse = { data: null };
                req.flush(mockResponse, mockErrorResponse);
            });

            it('if config does not match current site', (done) => {
                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/ema');

                const mockResponse = {
                    entity: {
                        sites: [
                            {
                                id: '123',
                                configured: false,
                                secrets: [
                                    {
                                        value: '[{}]'
                                    }
                                ]
                            }
                        ]
                    }
                };
                req.flush(mockResponse);
            });

            it('if pattern in value does not match url', (done) => {
                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/ema');

                const mockResponse = {
                    entity: {
                        sites: [
                            {
                                id: '123',
                                configured: true,
                                secrets: [
                                    {
                                        value: '[ { "pattern":"/blogs/(.*)", "url":"https://myspa.blogs.com:3000", "options": { "authenticationToken": "123", "depth": 3, "X-CONTENT-APP": "dotCMS" } } ]'
                                    }
                                ]
                            }
                        ]
                    }
                };
                req.flush(mockResponse);
            });

            it('if value is not valid json', (done) => {
                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/ema');

                const mockResponse = {
                    entity: {
                        sites: [
                            {
                                id: '123',
                                configured: true,
                                secrets: [
                                    {
                                        value: '[ { "pattern"", "url":"https://myspa.blogs.com:3000", "options": { "authenticationToken": "123", "depth": 3, "X-CONTENT-APP": "dotCMS" } } ]'
                                    }
                                ]
                            }
                        ]
                    }
                };
                req.flush(mockResponse);
            });
        });

        it('should return value', (done) => {
            spectator.service.get('test').subscribe((res) => {
                expect(res).toEqual({
                    options: { 'X-CONTENT-APP': 'dotCMS', authenticationToken: '123' },
                    pattern: '.*',
                    url: 'https://myspa.blogs.com:3000'
                });
                done();
            });

            const req = httpTestingController.expectOne('/api/v1/ema');

            const mockResponse = {
                entity: {
                    config: [
                        {
                            pattern: '.*',
                            url: 'https://myspa.blogs.com:3000',
                            options: { authenticationToken: '123', 'X-CONTENT-APP': 'dotCMS' }
                        }
                    ]
                }
            };
            req.flush(mockResponse);
        });
    });
});
