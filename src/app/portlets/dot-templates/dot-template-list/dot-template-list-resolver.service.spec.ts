import { TestBed } from '@angular/core/testing';

import { DotTemplateListResolver } from './dot-template-list-resolver.service';
import { OrderDirection, PaginatorService } from '@services/paginator';
import { ApiRoot, CoreWebService, LoggerService, StringUtils, UserModel } from 'dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { take } from 'rxjs/operators';
import { DotTemplate } from '@models/dot-edit-layout-designer';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { TEMPLATE_API_URL } from '@services/dot-templates/dot-templates.service';

describe('DotTemplateListResolverService', () => {
    let service: DotTemplateListResolver;
    let paginationService: PaginatorService;
    let pushPublishService: PushPublishService;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                PaginatorService,
                DotLicenseService,
                PushPublishService,
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                DotCurrentUserService,
                DotTemplateListResolver,
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        });
        service = TestBed.inject(DotTemplateListResolver);
        paginationService = TestBed.inject(PaginatorService);
        pushPublishService = TestBed.inject(PushPublishService);
        dotLicenseService = TestBed.inject(DotLicenseService);
    });

    it('should set pagination params, get first page, check license and publish environments', () => {
        const firstPage: DotTemplate[] = [
            {
                anonymous: true,
                friendlyName: 'A',
                identifier: 'id',
                inode: '11',
                name: 'a',
                type: '1',
                versionType: '1',
                canEdit: true,
                layout: null,
                canPublish: true,
                canWrite: true,
                hasLiveVersion: true,
                working: true
            }
        ];

        spyOn(paginationService, 'getFirstPage').and.returnValue(of(firstPage));
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
            .subscribe(([templates, isEnterPrise, hasEnvironments]) => {
                expect(templates).toEqual(firstPage);
                expect(isEnterPrise).toEqual(true);
                expect(hasEnvironments).toEqual(true);
            });
        expect(paginationService.url).toEqual(TEMPLATE_API_URL);
        expect(paginationService.sortField).toEqual('modDate');
        expect(paginationService.sortOrder).toEqual(OrderDirection.DESC);
        expect(paginationService.getFirstPage).toHaveBeenCalled();
    });
});
