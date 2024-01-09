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

            // it('if license isEnterprise throws error', (done) => {
            //     jest.spyOn(licenseService, 'isEnterprise').mockImplementation(() => {
            //         throw new Error('License is not enterprise');
            //     });

            //     spectator.service.get('test').subscribe(res => {
            //         console.log('test', res);

            //         expect(res).toBeNull();
            //         done();
            //     });
            // });

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
        });
    });
});
