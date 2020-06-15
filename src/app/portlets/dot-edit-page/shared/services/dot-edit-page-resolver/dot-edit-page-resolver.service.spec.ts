import { ActivatedRouteSnapshot } from '@angular/router';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotEditPageResolver } from './dot-edit-page-resolver.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotPageRender } from '../../models';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DotPageRenderState } from '../../models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { LoginService, HttpCode, ResponseView } from 'dotcms-js';
import { LoginServiceMock, mockUser } from './../../../../../test/login-service.mock';
import { Response, Headers } from '@angular/http';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { mockDotRenderedPage } from '../../../../../test/dot-page-render.mock';
import { mockResponseView } from '../../../../../test/response-view.mock';
import { throwError, of } from 'rxjs';

const route: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);

route.queryParams = {};

describe('DotEditPageResolver', () => {
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotPageStateService: DotPageStateService;
    let dotPageStateServiceRequestPageSpy: jasmine.Spy;
    let dotRouterService: DotRouterService;
    let resolver: DotEditPageResolver;

    beforeEach(async(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotHttpErrorManagerService,
                DotPageStateService,
                DotEditPageResolver,
                DotPageRenderService,
                DotContentletLockerService,
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: route
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            imports: [RouterTestingModule]
        });

        dotHttpErrorManagerService = testbed.get(DotHttpErrorManagerService);
        dotPageStateService = testbed.get(DotPageStateService);
        dotPageStateServiceRequestPageSpy = spyOn(dotPageStateService, 'requestPage');
        dotRouterService = testbed.get(DotRouterService);
        resolver = testbed.get(DotEditPageResolver);

        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
    }));

    beforeEach(() => {
        route.queryParams.url = 'edit-page/content';
        route.queryParams.language_id = '2';
        route.children = [
            {
                url: [
                    {
                        path: 'content'
                    }
                ]
            }
        ];
    });

    it('should return a DotRenderedPageState', () => {
        const mock = new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage));
        dotPageStateServiceRequestPageSpy.and.returnValue(of(mock));

        resolver.resolve(route).subscribe((state: DotPageRenderState) => {
            expect(state).toEqual(mock);
        });

        expect(dotPageStateService.requestPage).toHaveBeenCalledWith({
            url: 'edit-page/content',
            viewAs: {
                language: '2'
            }
        });
    });

    it('should redirect to site-browser when request fail', () => {
        const fake403Response = mockResponseView(403);

        dotPageStateServiceRequestPageSpy.and.returnValue(throwError(fake403Response));

        resolver.resolve(route).subscribe();
        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
    });

    it('should return DotPageRenderState from local state', () => {
        const mock = new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage));
        dotPageStateService.setInternalNavigationState(mock);

        resolver.resolve(route).subscribe((state: DotPageRenderState) => {
            expect(state).toEqual(mock);
        });

        expect(dotPageStateServiceRequestPageSpy).not.toHaveBeenCalled();
    });

    describe('handle layout', () => {
        beforeEach(() => {
            route.children = [
                {
                    url: [
                        {
                            path: 'layout'
                        }
                    ]
                }
            ];
        });

        it('should return a DotRenderedPageState', () => {
            const mock = new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage));
            dotPageStateServiceRequestPageSpy.and.returnValue(of(mock));

            resolver.resolve(route).subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });
            expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        });

        it('should handle error and redirect to site-browser when cant edit layout', () => {
            const mock = new DotPageRenderState(
                mockUser,
                new DotPageRender({
                    ...mockDotRenderedPage,
                    page: {
                        ...mockDotRenderedPage.page,
                        canEdit: false
                    }
                })
            );
            dotPageStateServiceRequestPageSpy.and.returnValue(of(mock));

            resolver.resolve(route).subscribe((state: DotPageRenderState) => {;
                expect(state).toBeNull();
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                new ResponseView(
                    new Response({
                        body: {},
                        status: HttpCode.FORBIDDEN,
                        headers: null,
                        url: '',
                        merge: null
                    })
                )
            );
        });

        it('should handle error and redirect to site-browser when layout is not drawed', () => {
            const mock = new DotPageRenderState(
                mockUser,
                new DotPageRender({
                    ...mockDotRenderedPage,
                    layout: null
                })
            );
            dotPageStateServiceRequestPageSpy.and.returnValue(of(mock));

            resolver.resolve(route).subscribe((state: DotPageRenderState) => {
                expect(state).toBeNull();
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                new ResponseView(
                    new Response({
                        body: {},
                        status: HttpCode.FORBIDDEN,
                        headers: new Headers({
                            'error-key': 'dotcms.api.error.license.required'
                        }),
                        url: '',
                        merge: null
                    })
                )
            );
        });
    });
});
