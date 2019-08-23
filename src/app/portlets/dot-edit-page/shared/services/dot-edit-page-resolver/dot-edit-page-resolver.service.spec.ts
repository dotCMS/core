import { throwError as observableThrowError, of as observableOf } from 'rxjs';
import { mockDotRenderedPage } from '../../../../../test/dot-page-render.mock';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotEditPageResolver } from './dot-edit-page-resolver.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock, mockUser } from './../../../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DotPageRenderState } from '../../models/dot-rendered-page-state.model';
import { mockResponseView } from '../../../../../test/response-view.mock';
import { DotPageRender } from '../../models';

const route: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);

route.queryParams = {};

describe('DotEditPageResolver', () => {
    let resolver: DotEditPageResolver;
    let dotPageStateService: DotPageStateService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotRouterService: DotRouterService;

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
                },
            ],
            imports: [RouterTestingModule]
        });

        resolver = testbed.get(DotEditPageResolver);
        dotPageStateService = testbed.get(DotPageStateService);
        dotHttpErrorManagerService = testbed.get(DotHttpErrorManagerService);
        dotRouterService = testbed.get(DotRouterService);
    }));

    describe('content', () => {
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

        it('should return a DotRenderedPageState valid object', () => {
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableOf(new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage)))
            );

            resolver.resolve(route).subscribe((res) => {
                expect(res).toEqual(new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage)));
            });
        });

        it('should return a DotRenderedPageState valid object when layout is null', () => {
            const mockDotRenderedPageState: DotPageRenderState = new DotPageRenderState(
                mockUser,
                new DotPageRender({
                    ...mockDotRenderedPage,
                    layout: null
                })
            );
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableOf(mockDotRenderedPageState)
            );

            resolver.resolve(route).subscribe((res) => {
                expect(res).toEqual(mockDotRenderedPageState);
            });
        });

        it('should return handle 403', () => {
            const fake403Response = mockResponseView(403);

            spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(
                observableOf({
                    redirected: true
                })
            );
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableThrowError(fake403Response)
            );

            resolver.resolve(route).subscribe();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(fake403Response);
            expect(dotPageStateService.get).toHaveBeenCalledWith({
                url: 'edit-page/content',
                viewAs: {
                    language_id: '2'
                }
            });
        });

        it('should redirect to site-browser', () => {
            const fake403Response = mockResponseView(403);

            spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(
                observableOf({
                    redirected: false
                })
            );
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableThrowError(fake403Response)
            );

            resolver.resolve(route).subscribe();
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
            expect(dotPageStateService.get).toHaveBeenCalledWith({
                url: 'edit-page/content',
                viewAs: {
                    language_id: '2'
                }
            });
        });
    });

    describe('layout', () => {
        beforeEach(() => {
            route.queryParams.url = 'edit-page/layout';
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

        it('should return a DotRenderedPageState valid object', () => {
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableOf(new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage)))
            );

            resolver.resolve(route).subscribe((res) => {
                expect(res).toEqual(new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage)));
            });
        });

        it('should trigger 403 error when try to go to layout because user canEdit page', () => {
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableOf(
                    new DotPageRenderState(mockUser,
                        new DotPageRender({
                            ...mockDotRenderedPage,
                            page: {
                                ...mockDotRenderedPage.page,
                                canEdit: false
                            }
                        })
                    )
                )
            );

            spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(
                observableOf({
                    redirected: false
                })
            );
            resolver.resolve(route).subscribe();
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should trigger 403 error when try to go to layout because layout is null', () => {
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableOf(
                    new DotPageRenderState(mockUser,
                        new DotPageRender({
                            ...mockDotRenderedPage,
                            layout: null
                        })
                    )
                )
            );

            spyOn(dotHttpErrorManagerService, 'handle').and.returnValue(
                observableOf({
                    redirected: false
                })
            );

            resolver.resolve(route).subscribe();
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });
    });

    describe('with dotRenderedPageState', () => {
        const renderedPageState: DotPageRenderState = new DotPageRenderState(
            mockUser,
            new DotPageRender(mockDotRenderedPage)
        );

        beforeEach(() => {
        });

        it('should return a DotRenderedPageState valid object', () => {
            resolver.resolve(route).subscribe((res) => {
                expect(res).toEqual(renderedPageState);
            });
        });
    });
});
