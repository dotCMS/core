import { createServiceFactory, SpectatorService } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { SiteService } from '@dotcms/dotcms-js';

import { EmaAppConfigurationService } from './ema-app-configuration.service';

import { DotLicenseService } from '../dot-license/dot-license.service';

describe('EmaAppConfigurationService', () => {
    let spectator: SpectatorService<EmaAppConfigurationService>;
    // let httpTestingController: HttpTestingController;

    const createService = createServiceFactory({
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
        // httpTestingController = spectator.inject(HttpTestingController);
    });

    describe('get', () => {
        it('should create', () => {
            expect(spectator.service).toBeTruthy();
        });
    });
});
