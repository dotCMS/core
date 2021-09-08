import { TestBed } from '@angular/core/testing';
import { DotTemplateListResolver } from './dot-template-list-resolver.service';
import { ApiRoot, CoreWebService, LoggerService, StringUtils, UserModel } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { take } from 'rxjs/operators';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotFormatDateService } from '@services/dot-format-date-service';

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
                DotFormatDateService
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
