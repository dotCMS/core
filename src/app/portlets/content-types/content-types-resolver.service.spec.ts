import { ContentTypeResolver } from './content-types-resolver.service';
import { TestBed, async } from '@angular/core/testing';
import { ContentTypesInfoService } from '../../api/services/content-types-info';
import { CrudService } from '../../api/services/crud';
import { LoginService } from 'dotcms-js/dotcms-js';
import { ActivatedRouteSnapshot, ParamMap } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { LoginServiceMock } from '../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { inject } from '@angular/core/testing';
import { DotRouterService } from '../../api/services/dot-router-service';

class CrudServiceMock {
    getDataById() {}
}

class DotRouterServiceMock {
    gotoPortlet() {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.paramMap = {};

describe('ContentTypeResolver', () => {
    let crudService: CrudService;
    let router: ActivatedRouteSnapshot;
    let contentTypeResolver: ContentTypeResolver;
    let dotRouterService: DotRouterService;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                providers: [
                    ContentTypeResolver,
                    ContentTypesInfoService,
                    {
                        provide: DotRouterService,
                        useClass: DotRouterServiceMock
                    },
                    { provide: CrudService, useClass: CrudServiceMock },
                    { provide: LoginService, useClass: LoginServiceMock },
                    {
                        provide: ActivatedRouteSnapshot,
                        useValue: activatedRouteSnapshotMock
                    }
                ],
                imports: [RouterTestingModule]
            });
            crudService = TestBed.get(CrudService);
            router = TestBed.get(ActivatedRouteSnapshot);
            contentTypeResolver = TestBed.get(ContentTypeResolver);
            dotRouterService = TestBed.get(DotRouterService);
        })
    );

    it('should get and return a content type', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';
        spyOn(crudService, 'getDataById').and.returnValue(
            Observable.of({
                fake: 'content-type',
                object: 'right?'
            })
        );

        contentTypeResolver.resolve(activatedRouteSnapshotMock).subscribe((fakeContentType: any) => {
            expect(fakeContentType).toEqual({
                fake: 'content-type',
                object: 'right?'
            });
        });
        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
    });

    it('should redirect to content-types if content type it\'s not found', () => {
        activatedRouteSnapshotMock.paramMap.get = () => 'invalid-id';

        spyOn(dotRouterService, 'gotoPortlet');

        spyOn(crudService, 'getDataById').and.returnValue(
            Observable.throw({})
        );

        contentTypeResolver.resolve(activatedRouteSnapshotMock).subscribe((fakeContentType: any) => {
            expect(fakeContentType).toEqual(null);
        });

        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', 'invalid-id');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular', true);
    });

    it('should get and return null and go to home', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';
        spyOn(dotRouterService, 'gotoPortlet');
        spyOn(crudService, 'getDataById').and.returnValue(Observable.of(false));
        contentTypeResolver.resolve(activatedRouteSnapshotMock).subscribe((res: any) => {
            expect(res).toBeNull();
        });
        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '123');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
    });

    it('should return a content type placeholder base on type', () => {
        activatedRouteSnapshotMock.paramMap.get = param => {
            return param === 'type' ? 'content' : false;
        };
        spyOn(dotRouterService, 'gotoPortlet');
        spyOn(crudService, 'getDataById').and.returnValue(Observable.of(false));
        contentTypeResolver.resolve(activatedRouteSnapshotMock).subscribe((res: any) => {
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
