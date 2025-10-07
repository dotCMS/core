import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { take } from 'rxjs/operators';

import {
    DotCurrentUserService,
    DotLicenseService,
    DotFormatDateService,
    PushPublishService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotContainerListResolver } from './dot-container-list-resolver.service';

describe('DotContainerListResolverService', () => {
    let service: DotContainerListResolver;
    let pushPublishService: PushPublishService;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotLicenseService,
                PushPublishService,
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                DotCurrentUserService,
                DotContainerListResolver,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotFormatDateService,
                {
                    provide: DotcmsConfigService,
                    useValue: {
                        getSystemTimeZone: () =>
                            of({
                                id: 'America/Costa_Rica',
                                label: 'Central Standard Time (America/Costa_Rica)',
                                offset: -21600000
                            })
                    }
                },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                }
            ]
        });
        service = TestBed.inject(DotContainerListResolver);
        pushPublishService = TestBed.inject(PushPublishService);
        dotLicenseService = TestBed.inject(DotLicenseService);
    });

    it('should set pagination params, get first page, check license and publish environments', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
        jest.spyOn(pushPublishService, 'getEnvironments').mockReturnValue(
            of([
                {
                    id: '1',
                    name: 'environment'
                }
            ])
        );
        service
            .resolve()
            .pipe(take(1))
            .subscribe(([isEnterPrise, hasEnvironments]) => {
                expect(isEnterPrise).toEqual(true);
                expect(hasEnvironments).toEqual(true);
            });
    });
});
