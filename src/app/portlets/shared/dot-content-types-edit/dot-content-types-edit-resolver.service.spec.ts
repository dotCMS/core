import { throwError as observableThrowError, of as observableOf } from 'rxjs';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotContentTypeEditResolver } from './dot-content-types-edit-resolver.service';
import { waitForAsync } from '@angular/core/testing';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DotCrudService } from '@services/dot-crud';
import { LoginService } from 'dotcms-js';
import { ActivatedRouteSnapshot } from '@angular/router';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotCMSContentType } from 'dotcms-models';

class CrudServiceMock {
    getDataById() {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('DotContentTypeEditResolver', () => {
    let crudService: DotCrudService;
    let dotContentTypeEditResolver: DotContentTypeEditResolver;
    let dotRouterService: DotRouterService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(waitForAsync( () => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotContentTypeEditResolver,
                DotContentTypesInfoService,
                DotHttpErrorManagerService,
                { provide: DotCrudService, useClass: CrudServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ],
            imports: [RouterTestingModule]
        });
        crudService = testbed.get(DotCrudService);
        dotContentTypeEditResolver = testbed.get(DotContentTypeEditResolver);
        dotRouterService = testbed.get(DotRouterService);
        dotHttpErrorManagerService = testbed.get(DotHttpErrorManagerService);
    }));

    it('should get and return a content type', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';
        spyOn(crudService, 'getDataById').and.returnValue(
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
    });

    it('should redirect to content-types if content type it\'s not found', () => {
        activatedRouteSnapshotMock.paramMap.get = () => 'invalid-id';

        spyOn<any>(dotHttpErrorManagerService, 'handle').and.returnValue(
            observableOf({
                redirected: false
            })
        );

        spyOn(crudService, 'getDataById').and.returnValue(
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
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular', true);
    });

    it('should get and return null and go to home', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';

        spyOn<any>(dotHttpErrorManagerService, 'handle').and.returnValue(
            observableOf({
                redirected: false
            })
        );

        spyOn(crudService, 'getDataById').and.returnValue(
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
        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular', true);
    });

    it('should return a content type placeholder base on type', () => {
        activatedRouteSnapshotMock.paramMap.get = (param) => {
            return param === 'type' ? 'content' : false;
        };

        spyOn(crudService, 'getDataById').and.returnValue(observableOf(false));
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
