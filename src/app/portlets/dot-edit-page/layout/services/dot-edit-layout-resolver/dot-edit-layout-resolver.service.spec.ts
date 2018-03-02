import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { PageViewService } from '../../../../../api/services/page-view/page-view.service';
import { async } from '@angular/core/testing';
import { EditLayoutResolver } from './dot-edit-layout-resolver.service';
import { PageViewServiceMock } from '../../../../../test/page-view.mock';
import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { LoginService } from 'dotcms-js/dotcms-js';

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.queryParams = {};

describe('EditLayoutResolver', () => {
    let pageViewResolver: EditLayoutResolver;
    let pageViewService: PageViewService;

    beforeEach(
        async(() => {
            const testbed = DOTTestBed.configureTestingModule({
                providers: [
                    EditLayoutResolver,
                    DotHttpErrorManagerService,
                    DotRouterService,
                    { provide: PageViewService, useClass: PageViewServiceMock },
                    { provide: LoginService, useClass: LoginServiceMock },
                    {
                        provide: ActivatedRouteSnapshot,
                        useValue: activatedRouteSnapshotMock
                    }
                ],
                imports: [RouterTestingModule]
            });

            pageViewService = testbed.get(PageViewService);
            pageViewResolver = testbed.get(EditLayoutResolver);
        })
    );

    it('should do a resolve and return an object', () => {
        let result: any;

        spyOn(pageViewService, 'get').and.returnValue(
            Observable.of({
                object: 'Fake object'
            })
        );

        pageViewResolver.resolve(activatedRouteSnapshotMock).subscribe((fakeRes: any) => (result = fakeRes));

        expect(result).toEqual({ object: 'Fake object' });
    });
});
