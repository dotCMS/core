import { TestBed } from '@angular/core/testing';
import { DotTemplateListResolver } from './dot-template-list-resolver.service';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    LoggerService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { DotLicenseService } from '@dotcms/data-access';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { take } from 'rxjs/operators';
import { DotCurrentUserService } from '@dotcms/data-access';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';

describe('DotTemplateListResolverService', () => {
    let service: DotTemplateListResolver;
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
                DotTemplateListResolver,
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
                }
            ]
        });
        service = TestBed.inject(DotTemplateListResolver);
        pushPublishService = TestBed.inject(PushPublishService);
        dotLicenseService = TestBed.inject(DotLicenseService);
    });

    it('should set pagination params, get first page, check license and publish environments', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
        spyOn(pushPublishService, 'getEnvironments').and.returnValue(
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
