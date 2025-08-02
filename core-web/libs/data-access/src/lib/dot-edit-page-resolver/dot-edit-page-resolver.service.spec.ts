/* eslint-disable @typescript-eslint/no-explicit-any */

import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { CoreWebService, HttpCode, LoginService, SiteService } from '@dotcms/dotcms-js';
import { DotPageMode, DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    DotLicenseServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotHttpErrorManagerService,
    mockDotRenderedPage,
    MockDotRouterJestService,
    mockResponseView,
    mockUser,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotEditPageResolver } from './dot-edit-page-resolver.service';

import { DotAlertConfirmService } from '../dot-alert-confirm/dot-alert-confirm.service';
import { DotContentletLockerService } from '../dot-contentlet-locker/dot-contentlet-locker.service';
import { DotESContentService } from '../dot-es-content/dot-es-content.service';
import { DotExperimentsService } from '../dot-experiments/dot-experiments.service';
import { DotFavoritePageService } from '../dot-favorite-page/dot-favorite-page.service';
import { DotFormatDateService } from '../dot-format-date/dot-format-date.service';
import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';
import { DotLicenseService } from '../dot-license/dot-license.service';
import { DotMessageDisplayService } from '../dot-message-display/dot-message-display.service';
import { DotPageRenderService } from '../dot-page-render/dot-page-render.service';
import { DotPageStateService } from '../dot-page-state/dot-page-state.service';
import { DotRouterService } from '../dot-router/dot-router.service';
import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

const route: any = jest.spyOn(ActivatedRouteSnapshot, 'toString');

route.queryParams = {};

describe('DotEditPageResolver', () => {
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotPageStateService: DotPageStateService;
    let dotPageStateServiceRequestPageSpy: jest.SpyInstance;
    let dotRouterService: DotRouterService;
    let dotSessionStorageService: DotSessionStorageService;

    let injector: TestBed;
    let dotEditPageResolver: DotEditPageResolver;
    let siteService: SiteService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSessionStorageService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                },
                DotPageStateService,
                DotExperimentsService,
                DotEditPageResolver,
                DotPageRenderService,
                DotContentletLockerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotFormatDateService,
                DotESContentService,
                DotFavoritePageService,
                { provide: DotRouterService, useValue: new MockDotRouterJestService(jest) },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: SiteService, useClass: SiteServiceMock },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: route
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotLicenseService,
                    useClass: DotLicenseServiceMock
                }
            ]
        });
        injector = getTestBed();
        dotEditPageResolver = injector.inject(DotEditPageResolver);
        dotHttpErrorManagerService = injector.inject(DotHttpErrorManagerService);
        dotPageStateService = injector.inject(DotPageStateService);
        dotPageStateServiceRequestPageSpy = jest.spyOn(dotPageStateService, 'requestPage');
        dotRouterService = injector.inject(DotRouterService);
        siteService = injector.inject(SiteService);
        dotSessionStorageService = injector.inject(DotSessionStorageService);

        jest.spyOn(dotHttpErrorManagerService, 'handle').mockReturnValue(of());
    });

    beforeEach(() => {
        route.queryParams.url = 'edit-page/content';
        route.queryParams.language_id = '2';
        route.queryParams.mode = DotPageMode.EDIT;
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
        const mock = new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()));
        dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));

        dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState | null) => {
            expect(state).toEqual(mock);
        });

        expect<any>(dotPageStateService.requestPage).toHaveBeenCalledWith({
            url: 'edit-page/content',
            mode: DotPageMode.EDIT,
            viewAs: {
                language: '2'
            }
        });
    });

    it('should redirect to site-browser when request fail', () => {
        const fake403Response = mockResponseView(403);

        dotPageStateServiceRequestPageSpy.mockReturnValue(throwError(fake403Response));

        dotEditPageResolver.resolve(route).subscribe();
        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
    });

    it('should return DotPageRenderState from local state', () => {
        const mock = new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()));
        dotPageStateService.setInternalNavigationState(mock);

        dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState | null) => {
            expect(state).toEqual(mock);
        });

        expect(dotPageStateServiceRequestPageSpy).not.toHaveBeenCalled();
    });

    describe('Switch Site', () => {
        it('should switch site when host_id is present in queryparams', () => {
            route.queryParams.host_id = '123';
            jest.spyOn(siteService, 'switchSiteById').mockReturnValue(of());
            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage())
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
            dotEditPageResolver.resolve(route).subscribe();
            expect(siteService.switchSiteById).toHaveBeenCalledWith('123');
        });

        it('should not switch site when host_id is not present in queryparams', () => {
            route.queryParams = {};
            jest.spyOn(siteService, 'switchSiteById').mockReturnValue(of());
            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage())
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
            dotEditPageResolver.resolve(route).subscribe();
            expect(siteService.switchSiteById).not.toHaveBeenCalled();
        });

        it('should not switch site when host_id is equal to current site id', () => {
            route.queryParams.host_id = siteService.currentSite.identifier;
            jest.spyOn(siteService, 'switchSiteById').mockReturnValue(of());
            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage())
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));
            dotEditPageResolver.resolve(route).subscribe();
            expect(siteService.switchSiteById).not.toHaveBeenCalled();
        });
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
            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage())
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));

            dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState | null) => {
                expect(state).toEqual(mock);
            });
            expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        });

        it('should handle error and redirect to site-browser when cant edit layout', () => {
            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    ...mockDotRenderedPage(),
                    page: {
                        ...mockDotRenderedPage().page,
                        canEdit: false
                    }
                })
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));

            dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState | null) => {
                expect(state).toBeNull();
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                new HttpErrorResponse({
                    status: HttpCode.FORBIDDEN,
                    url: ''
                })
            );
        });

        it('should call to `removeVariantId` when handle error and redirect to site-browser ', () => {
            jest.spyOn(dotSessionStorageService, 'removeVariantId');

            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    ...mockDotRenderedPage(),
                    page: {
                        ...mockDotRenderedPage().page,
                        canEdit: false
                    }
                })
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));

            dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState | null) => {
                expect(state).toBeNull();
            });

            expect(dotSessionStorageService.removeVariantId).toHaveBeenCalled();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                new HttpErrorResponse({
                    status: HttpCode.FORBIDDEN,
                    url: ''
                })
            );
        });

        it('should handle error and redirect to site-browser when layout is not drawed', () => {
            const mock = new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    ...mockDotRenderedPage(),
                    layout: undefined
                })
            );
            dotPageStateServiceRequestPageSpy.mockReturnValue(of(mock));

            dotEditPageResolver.resolve(route).subscribe((state: DotPageRenderState | null) => {
                expect(state).toBeNull();
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        });
    });
});
