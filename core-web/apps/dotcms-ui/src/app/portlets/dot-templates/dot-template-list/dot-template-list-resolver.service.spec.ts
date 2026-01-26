import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { take } from 'rxjs/operators';

import {
    DotCurrentUserService,
    PushPublishService,
    DotFormatDateService
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

import { DotTemplateListResolver } from './dot-template-list-resolver.service';

describe('DotTemplateListResolverService', () => {
    let service: DotTemplateListResolver;
    let pushPublishService: PushPublishService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
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
                },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                }
            ]
        });
        service = TestBed.inject(DotTemplateListResolver);
        pushPublishService = TestBed.inject(PushPublishService);
    });

    it('should set pagination params, get first page, check license and publish environments', () => {
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
            .subscribe((hasEnvironments: boolean) => {
                expect(hasEnvironments).toEqual(true);
            });
    });
});
