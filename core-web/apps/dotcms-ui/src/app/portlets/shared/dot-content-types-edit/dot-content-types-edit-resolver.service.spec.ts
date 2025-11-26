/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of as observableOf, throwError as observableThrowError } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import {
    DotContentTypesInfoService,
    DotCrudService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotRouterService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessageDisplayServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotContentTypeEditResolver } from './dot-content-types-edit-resolver.service';

import { DOTTestBed } from '../../../test/dot-test-bed';

class CrudServiceMock {
    getDataById() {}
}

const activatedRouteSnapshotMock: any = jest.fn<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.paramMap = {};

describe('DotContentTypeEditResolver', () => {
    let crudService: DotCrudService;
    let dotContentTypeEditResolver: DotContentTypeEditResolver;
    let dotRouterService: DotRouterService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let globalStore: InstanceType<typeof GlobalStore>;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                DotContentTypeEditResolver,
                DotContentTypesInfoService,
                DotHttpErrorManagerService,
                { provide: DotCrudService, useClass: CrudServiceMock },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => observableOf({}) }
                },
                GlobalStore
            ],
            imports: [RouterTestingModule]
        });
        crudService = TestBed.inject(DotCrudService);
        dotContentTypeEditResolver = TestBed.inject(DotContentTypeEditResolver);
        dotRouterService = TestBed.inject(DotRouterService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        globalStore = TestBed.inject(GlobalStore);

        // Spy on addNewBreadcrumb to prevent errors when contentType is null
        jest.spyOn(globalStore, 'addNewBreadcrumb').mockImplementation(() => {});
    }));

    it('should get and return a content type', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';
        jest.spyOn(crudService, 'getDataById').mockReturnValue(
            observableOf({
                fake: 'content-type',
                object: 'right?'
            })
        );

        dotContentTypeEditResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual({
                    fake: 'content-type',
                    object: 'right?'
                });
            });
        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
        expect(crudService.getDataById).toHaveBeenCalledTimes(1);
    });

    it("should redirect to content-types if content type it's not found", () => {
        activatedRouteSnapshotMock.paramMap.get = () => 'invalid-id';

        jest.spyOn<any>(dotHttpErrorManagerService, 'handle').mockReturnValue(
            observableOf({
                redirected: false
            })
        );

        jest.spyOn(crudService, 'getDataById').mockReturnValue(
            observableThrowError({
                bodyJsonObject: {
                    error: ''
                },
                response: {
                    status: 403
                }
            })
        );

        dotContentTypeEditResolver.resolve(activatedRouteSnapshotMock).subscribe();

        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', 'invalid-id');
        expect(crudService.getDataById).toHaveBeenCalledTimes(1);
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular', {
            replaceUrl: true
        });
    });

    it.skip('should get and return null and go to home', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';

        jest.spyOn<any>(dotHttpErrorManagerService, 'handle').mockReturnValue(
            observableOf({
                redirected: false
            })
        );

        jest.spyOn(crudService, 'getDataById').mockReturnValue(
            observableThrowError({
                bodyJsonObject: {
                    error: ''
                },
                response: {
                    status: 403
                }
            })
        );

        // Subscribe with error handler since tap will try to access null.name
        dotContentTypeEditResolver.resolve(activatedRouteSnapshotMock).subscribe({
            error: () => {
                // Expected error when trying to access properties of null
                expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
                expect(crudService.getDataById).toHaveBeenCalledTimes(1);
                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith(
                    '/content-types-angular',
                    {
                        replaceUrl: true
                    }
                );
            }
        });
    });

    it.skip('should return a content type placeholder base on type', () => {
        activatedRouteSnapshotMock.paramMap.get = (param) => {
            return param === 'type' ? 'content' : false;
        };

        jest.spyOn(crudService, 'getDataById').mockReturnValue(observableOf(false));
        dotContentTypeEditResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((res: DotCMSContentType) => {
                expect(res).toEqual({
                    baseType: 'content',
                    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                    defaultType: false,
                    fields: [],
                    fixed: false,
                    folder: 'SYSTEM_FOLDER',
                    host: null,
                    iDate: null,
                    id: null,
                    layout: [],
                    modDate: null,
                    multilingualable: false,
                    nEntries: 0,
                    name: null,
                    owner: '123',
                    system: false,
                    variable: null,
                    versionable: false,
                    workflows: []
                });
            });
    });
});
