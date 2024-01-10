import { createHttpFactory, SpectatorService } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { SiteService } from '@dotcms/dotcms-js';

import { EmaAppConfigurationService } from './ema-app-configuration.service';

import { DotLicenseService } from '../dot-license/dot-license.service';

describe('EmaAppConfigurationService', () => {
    let spectator: SpectatorService<EmaAppConfigurationService>;
    let licenseService: DotLicenseService;
    let siteService: SiteService;
    let httpTestingController: HttpTestingController;

    const createService = createHttpFactory({
        service: EmaAppConfigurationService,
        imports: [HttpClientTestingModule, RouterTestingModule],
        providers: [
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise() {
                        return of(true);
                    }
                }
            },
            {
                provide: SiteService,
                useValue: {
                    getCurrentSite() {
                        return of({
                            identifier: '123'
                        });
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        licenseService = spectator.inject(DotLicenseService);
        siteService = spectator.inject(SiteService);
        httpTestingController = spectator.inject(HttpTestingController);

        jest.resetAllMocks(); // Reset all mocks before each test
    });

    describe('get', () => {
        describe('should return null', () => {
            it('if license isEnterprise is `false`', (done) => {
                jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(false));

                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();

                    done();
                });
            });

            it('if siteService getCurrentSite throws error', (done) => {
                jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
                jest.spyOn(siteService, 'getCurrentSite').mockImplementation(() => {
                    throw new Error('getCurrentSite error');
                });

                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });
            });

            it('if get app config return bad request', (done) => {
                jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
                jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                    of({
                        identifier: '123'
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    }) as any
                );

                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();

                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/apps/dotema-config-v2/123');

                const mockErrorResponse = { status: 400, statusText: 'Bad Request' };
                const mockResponse = { data: null };
                req.flush(mockResponse, mockErrorResponse);
            });

            it('if config does not match current site', (done) => {
                jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
                jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                    of({
                        identifier: '123'
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    }) as any
                );

                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/apps/dotema-config-v2/123');

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
                jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
                jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                    of({
                        identifier: '123'
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    }) as any
                );

                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/apps/dotema-config-v2/123');

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
                jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
                jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                    of({
                        identifier: '123'
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    }) as any
                );

                spectator.service.get('test').subscribe((res) => {
                    expect(res).toBeNull();
                    done();
                });

                const req = httpTestingController.expectOne('/api/v1/apps/dotema-config-v2/123');

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
            jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
            jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                of({
                    identifier: '123'
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                }) as any
            );

            spectator.service.get('test').subscribe((res) => {
                expect(res).toEqual({
                    options: { 'X-CONTENT-APP': 'dotCMS', authenticationToken: '123' },
                    pattern: '(.*)',
                    url: 'https://myspa.blogs.com:3000'
                });
                done();
            });

            const req = httpTestingController.expectOne('/api/v1/apps/dotema-config-v2/123');

            const mockResponse = {
                entity: {
                    sites: [
                        {
                            id: '123',
                            configured: true,
                            secrets: [
                                {
                                    value: '[ { "pattern":"(.*)", "url":"https://myspa.blogs.com:3000", "options": { "authenticationToken": "123", "X-CONTENT-APP": "dotCMS" } } ]'
                                }
                            ]
                        }
                    ]
                }
            };
            req.flush(mockResponse);
        });

        it('should return value even when the url have trailing and leading slashes', (done) => {
            jest.spyOn(licenseService, 'isEnterprise').mockReturnValue(of(true));
            jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                of({
                    identifier: '123'
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                }) as any
            );

            spectator.service.get('/blog/test/').subscribe((res) => {
                expect(res).toEqual({
                    options: { 'X-CONTENT-APP': 'dotCMS', authenticationToken: '123' },
                    pattern: '/blog/(.*)/',
                    url: 'https://myspa.blogs.com:3000'
                });
                done();
            });

            const req = httpTestingController.expectOne('/api/v1/apps/dotema-config-v2/123');

            const mockResponse = {
                entity: {
                    sites: [
                        {
                            id: '123',
                            configured: true,
                            secrets: [
                                {
                                    value: '[ { "pattern":"/blog/(.*)/", "url":"https://myspa.blogs.com:3000", "options": { "authenticationToken": "123", "X-CONTENT-APP": "dotCMS" } } ]'
                                }
                            ]
                        }
                    ]
                }
            };
            req.flush(mockResponse);
        });
    });
});
