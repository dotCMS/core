import { throwError as observableThrowError, of as observableOf } from 'rxjs';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ContentTypeEditResolver } from './content-types-edit-resolver.service';
import { async } from '@angular/core/testing';
import { ContentTypesInfoService } from '@services/content-types-info';
import { CrudService } from '@services/crud';
import { LoginService } from 'dotcms-js';
import { ActivatedRouteSnapshot } from '@angular/router';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DOTTestBed } from '../../../test/dot-test-bed';

class CrudServiceMock {
    getDataById() {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

describe('ContentTypeEditResolver', () => {
    let crudService: CrudService;
    let contentTypeEditResolver: ContentTypeEditResolver;
    let dotRouterService: DotRouterService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                ContentTypeEditResolver,
                ContentTypesInfoService,
                DotHttpErrorManagerService,
                { provide: CrudService, useClass: CrudServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ],
            imports: [RouterTestingModule]
        });
        crudService = testbed.get(CrudService);
        contentTypeEditResolver = testbed.get(ContentTypeEditResolver);
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

        contentTypeEditResolver
            .resolve(activatedRouteSnapshotMock)
            .subscribe((fakeContentType: any) => {
                expect(fakeContentType).toEqual({
                    fake: 'content-type',
                    object: 'right?'
                });
            });
        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
    });

    it("should redirect to content-types if content type it's not found", () => {
        activatedRouteSnapshotMock.paramMap.get = () => 'invalid-id';

        spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(
            observableOf({
                redirected: false
            })
        );

        spyOn(dotRouterService, 'gotoPortlet');

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

        contentTypeEditResolver.resolve(activatedRouteSnapshotMock).subscribe();

        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', 'invalid-id');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular', true);
    });

    it('should get and return null and go to home', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';

        spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(
            observableOf({
                redirected: false
            })
        );
        spyOn(dotRouterService, 'gotoPortlet');
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

        contentTypeEditResolver.resolve(activatedRouteSnapshotMock).subscribe();
        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular', true);
    });

    it('should return a content type placeholder base on type', () => {
        activatedRouteSnapshotMock.paramMap.get = (param) => {
            return param === 'type' ? 'content' : false;
        };
        spyOn(dotRouterService, 'gotoPortlet');
        spyOn(crudService, 'getDataById').and.returnValue(observableOf(false));
        contentTypeEditResolver.resolve(activatedRouteSnapshotMock).subscribe((res: any) => {
            expect(res).toEqual({
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                host: null,
                name: null,
                owner: '123',
                system: false,
                baseType: 'CONTENT'
            });
        });
    });
});
