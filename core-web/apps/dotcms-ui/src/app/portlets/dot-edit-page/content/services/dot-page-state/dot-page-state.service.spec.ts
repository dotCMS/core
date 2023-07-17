import { of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotFavoritePageService } from '@dotcms/app/api/services/dot-favorite-page/dot-favorite-page.service';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import {
    DotAlertConfirmService,
    DotContentletLockerService,
    DotESContentService,
    DotLicenseService,
    DotPageRenderService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { CoreWebService, HttpCode, LoginService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotDevice,
    DotExperimentStatus,
    DotPageMode,
    DotPageRenderState,
    DotPersona
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    getExperimentMock,
    LoginServiceMock,
    mockDotPersona,
    mockDotRenderedPage,
    MockDotRouterService,
    mockResponseView,
    mockUser,
    mockUserAuth
} from '@dotcms/utils-testing';
import { DotLicenseServiceMock } from '@portlets/dot-edit-page/content/services/html/dot-edit-content-toolbar-html.service.spec';

import { DotPageStateService } from './dot-page-state.service';

import { PageModelChangeEventType } from '../dot-edit-content-html/models';

const EXPERIMENT_MOCK = getExperimentMock(0);
const getDotPageRenderStateMock = (favoritePage?: DotCMSContentlet, runningExperiment = null) => {
    return new DotPageRenderState(
        mockUser(),
        mockDotRenderedPage(),
        favoritePage,
        runningExperiment
    );
};

describe('DotPageStateService', () => {
    let dotContentletLockerService: DotContentletLockerService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotHttpErrorManagerServiceHandle: jasmine.Spy;
    let dotPageRenderService: DotPageRenderService;
    let dotPageRenderServiceGetSpy: jasmine.Spy;
    let dotRouterService: DotRouterService;
    let dotFavoritePageService: DotFavoritePageService;
    let loginService: LoginService;
    let injector: TestBed;
    let service: DotPageStateService;
    let dotExperimentsService: DotExperimentsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSessionStorageService,
                DotContentletLockerService,
                DotHttpErrorManagerService,
                DotPageRenderService,
                DotPageStateService,
                DotAlertConfirmService,
                ConfirmationService,
                DotFormatDateService,
                DotESContentService,
                DotFavoritePageService,
                DotExperimentsService,
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
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
        service = injector.get(DotPageStateService);
        dotContentletLockerService = injector.get(DotContentletLockerService);
        dotHttpErrorManagerService = injector.get(DotHttpErrorManagerService);
        dotPageRenderService = injector.get(DotPageRenderService);
        dotRouterService = injector.get(DotRouterService);
        loginService = injector.get(LoginService);
        dotFavoritePageService = injector.get(DotFavoritePageService);
        dotExperimentsService = injector.get(DotExperimentsService);

        dotPageRenderServiceGetSpy = spyOn(dotPageRenderService, 'get').and.returnValue(
            of(mockDotRenderedPage())
        );

        dotHttpErrorManagerServiceHandle = spyOn(
            dotHttpErrorManagerService,
            'handle'
        ).and.callThrough();

        service = injector.get(DotPageStateService);

        spyOnProperty(dotRouterService, 'queryParams', 'get').and.returnValue({
            url: '/an/url/test/form/query/params'
        });

        spyOn(dotFavoritePageService, 'get').and.returnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: []
                },
                queryTook: 1,
                resultsSize: 20
            })
        );

        spyOn(dotExperimentsService, 'getByStatus').and.returnValue(of([]));
    });

    describe('Method: get', () => {
        it('should get with url', () => {
            service.get({
                url: 'some/url/test'
            });

            expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual([
                {
                    url: 'some/url/test'
                },
                {}
            ]);
        });

        it('should get with url from queryParams', () => {
            service.get();

            expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual([
                {
                    url: '/an/url/test/form/query/params'
                },
                {}
            ]);

            expect(dotFavoritePageService.get).toHaveBeenCalledWith({
                limit: 10,
                userId: 'dotcms.org.1',
                url: '/an/url/test?&language_id=1&device_inode='
            });
        });

        it('should get with url from queryParams with a Failing fetch from ES Search (favorite page)', () => {
            const error500 = mockResponseView(500, '/test', null, { message: 'error' });
            dotFavoritePageService.get = jasmine.createSpy().and.returnValue(throwError(error500));
            service.get();

            const subscribeCallback = jasmine.createSpy('spy');
            service.haveContent$.subscribe(subscribeCallback);

            expect(subscribeCallback).toHaveBeenCalledWith(true);
            expect(subscribeCallback).toHaveBeenCalledTimes(1);

            const mock = getDotPageRenderStateMock();
            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });
        });
    });

    describe('Get Running Experiment', () => {
        it('should get running experiment', () => {
            const mock = getDotPageRenderStateMock(null, EXPERIMENT_MOCK);
            dotExperimentsService.getByStatus = jasmine
                .createSpy()
                .and.returnValue(of([EXPERIMENT_MOCK]));

            service.get();

            expect(dotExperimentsService.getByStatus).toHaveBeenCalledWith(
                '123',
                DotExperimentStatus.RUNNING
            );

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });
        });

        it('should set running experiment to  null if no running experiments', () => {
            const mock = getDotPageRenderStateMock();

            service.get();

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });
        });

        it('should set running experiment to null if endpoint error', () => {
            const error500 = mockResponseView(500, '/test', null, {
                message: 'experiments.error.fetching.data'
            });
            const mock = getDotPageRenderStateMock();

            dotExperimentsService.getByStatus = jasmine
                .createSpy()
                .and.returnValue(throwError(error500));

            service.get();

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });
            expect(dotHttpErrorManagerServiceHandle).toHaveBeenCalledWith(error500, true);
        });
    });

    describe('$state', () => {
        it('should get state', () => {
            const mock = getDotPageRenderStateMock();
            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });
            service.get({
                url: 'some/url/test'
            });
        });

        it('should reload', () => {
            service.get({
                url: '/an/url/test/form/query/params'
            });
            service.reload();
            expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual([
                {
                    mode: 'PREVIEW_MODE',
                    url: '/an/url/test/form/query/params'
                },
                {}
            ]);
        });
    });

    describe('setters', () => {
        beforeEach(() => {
            service.get({
                url: 'some/url/test'
            });
        });

        describe('setLock', () => {
            it('should lock', () => {
                spyOn(dotContentletLockerService, 'lock').and.returnValue(
                    of({
                        id: '',
                        inode: '',
                        message: 'locked'
                    })
                );
                service.setLock(
                    {
                        mode: DotPageMode.LIVE
                    },
                    true
                );

                expect(dotContentletLockerService.lock).toHaveBeenCalledWith('2');
                expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual([
                    {
                        mode: 'ADMIN_MODE',
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                ]);
            });

            it('should unlock', () => {
                spyOn(dotContentletLockerService, 'unlock').and.returnValue(
                    of({
                        id: '',
                        inode: '',
                        message: 'unlocked'
                    })
                );
                service.setLock(
                    {
                        mode: DotPageMode.PREVIEW
                    },
                    false
                );

                expect(dotContentletLockerService.unlock).toHaveBeenCalledWith('2');
                expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual([
                    {
                        mode: 'PREVIEW_MODE',
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                ]);
            });
        });

        describe('setDevice', () => {
            it('should set iPad', () => {
                const device: DotDevice = {
                    inode: '',
                    name: 'iPad',
                    cssHeight: '',
                    cssWidth: '',
                    identifier: ''
                };
                service.setDevice(device);

                expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual(
                    [
                        {
                            viewAs: {
                                device: device
                            },
                            url: '/an/url/test/form/query/params'
                        },
                        {}
                    ],
                    {}
                );
            });
        });

        describe('setLanguage', () => {
            it('should set laguage 1', () => {
                service.setLanguage(1);
                expect(dotRouterService.replaceQueryParams).toHaveBeenCalledWith({
                    language_id: 1
                });
            });
        });

        describe('setFavoritePageHighlight', () => {
            it('should set FavoritePageHighlight', () => {
                service.state$.subscribe(({ state }: DotPageRenderState) => {
                    expect(state.favoritePage).toBe(dotcmsContentletMock);
                });
                service.setFavoritePageHighlight(dotcmsContentletMock);
            });

            it('should set FavoritePageHighlight false', () => {
                service.state$.subscribe(({ state }: DotPageRenderState) => {
                    expect(state.favoritePage).toBe(null);
                });
                service.setFavoritePageHighlight(null);
            });
        });

        describe('setPersona', () => {
            it('should set persona', () => {
                const persona: DotPersona = {
                    ...dotcmsContentletMock,
                    keyTag: 'persona',
                    name: 'persona',
                    personalized: false
                };
                service.setPersona(persona);

                expect(dotPageRenderServiceGetSpy.calls.mostRecent().args).toEqual([
                    {
                        viewAs: {
                            persona: persona
                        },
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                ]);
            });
        });
    });

    describe('errors', () => {
        it('should show error 300 message and redirect to site browser', () => {
            const error300 = mockResponseView(300);
            dotPageRenderServiceGetSpy.and.returnValue(throwError(error300));
            dotHttpErrorManagerServiceHandle.and.returnValue(
                of({
                    redirected: false,
                    status: HttpCode.FORBIDDEN
                })
            );

            service
                .requestPage({
                    url: 'hello/world'
                })
                .subscribe();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error300);
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should show error 404 message and redirect to site browser', () => {
            const error404 = mockResponseView(400);
            dotPageRenderServiceGetSpy.and.returnValue(throwError(error404));
            dotHttpErrorManagerServiceHandle.and.returnValue(
                of({
                    redirected: false,
                    status: HttpCode.NOT_FOUND
                })
            );

            service
                .requestPage({
                    url: 'hello/world'
                })
                .subscribe();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error404);
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should show error 500 and reload', () => {
            spyOn(service, 'reload');
            const error500 = mockResponseView(500);
            dotPageRenderServiceGetSpy.and.returnValue(throwError(error500));
            dotHttpErrorManagerServiceHandle.and.returnValue(
                of({
                    redirected: false,
                    status: HttpCode.SERVER_ERROR
                })
            );

            service
                .requestPage({
                    url: 'hello/world'
                })
                .subscribe();

            expect(service.reload).toHaveBeenCalledTimes(1);
            expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        });
    });

    describe('internal navigation state', () => {
        it('should return content from setted internal state', () => {
            const renderedPage = getDotPageRenderStateMock(dotcmsContentletMock);
            service.setInternalNavigationState(renderedPage);

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(renderedPage);
            });

            expect(service.getInternalNavigationState()).toEqual(renderedPage);
            expect(dotPageRenderServiceGetSpy).not.toHaveBeenCalled();
            expect(dotFavoritePageService.get).toHaveBeenCalledWith({
                limit: 10,
                userId: '123',
                url: '/an/url/test?&language_id=1&device_inode='
            });
        });

        it('should return null when internal state is not set', () => {
            expect(service.getInternalNavigationState()).toEqual(null);
            expect(dotPageRenderServiceGetSpy).not.toHaveBeenCalled();
        });
    });

    describe('setting local state', () => {
        it('should set local state and emit', () => {
            const renderedPage = getDotPageRenderStateMock(dotcmsContentletMock);

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(renderedPage);
            });

            service.setLocalState(renderedPage);
        });
    });

    describe('login as user', () => {
        beforeEach(() => {
            spyOnProperty(loginService, 'auth', 'get').and.returnValue(mockUserAuth);
        });

        it('should set lockedByAnotherUser', () => {
            service.state$.subscribe(({ state }: DotPageRenderState) => {
                expect(state.lockedByAnotherUser).toBe(true);
                expect(state.locked).toBe(true);
            });

            service.get({
                url: '/test/123'
            });
        });
    });

    describe('content added/removed', () => {
        describe('selected persona is not default', () => {
            it('should trigger haceContent as true', () => {
                const renderedPage = getDotPageRenderStateMock(dotcmsContentletMock);
                service.setLocalState(renderedPage);

                const subscribeCallback = jasmine.createSpy('spy');
                service.haveContent$.subscribe(subscribeCallback);

                expect(subscribeCallback).toHaveBeenCalledWith(true);

                service.updatePageStateHaveContent({
                    type: PageModelChangeEventType.REMOVE_CONTENT,
                    model: []
                });

                expect(subscribeCallback).toHaveBeenCalledWith(false);
                expect(subscribeCallback).toHaveBeenCalledTimes(2);
            });

            it('should trigger haceContent as false', () => {
                const renderedPage = new DotPageRenderState(mockUser(), {
                    ...mockDotRenderedPage(),
                    ...{
                        numberContents: 0
                    }
                });
                service.setLocalState(renderedPage);

                const subscribeCallback = jasmine.createSpy('spy');
                service.haveContent$.subscribe(subscribeCallback);

                expect(subscribeCallback).toHaveBeenCalledWith(false);

                service.updatePageStateHaveContent({
                    type: PageModelChangeEventType.ADD_CONTENT,
                    model: []
                });

                expect(subscribeCallback).toHaveBeenCalledWith(true);
                expect(subscribeCallback).toHaveBeenCalledTimes(2);
            });
        });

        describe('selected persona is not default', () => {
            it('should trigger haveContent as false', () => {
                const renderedPage = new DotPageRenderState(mockUser(), {
                    ...mockDotRenderedPage(),
                    ...{
                        viewAs: {
                            ...mockDotRenderedPage().viewAs,
                            persona: mockDotPersona
                        }
                    }
                });
                service.setLocalState(renderedPage);

                const subscribeCallback = jasmine.createSpy('spy');
                service.haveContent$.subscribe(subscribeCallback);

                expect(subscribeCallback).toHaveBeenCalledWith(false);

                service.updatePageStateHaveContent({
                    type: PageModelChangeEventType.REMOVE_CONTENT,
                    model: []
                });

                expect(subscribeCallback).toHaveBeenCalledTimes(1);
            });

            it('should trigger haceContent as true', () => {
                const renderedPage = new DotPageRenderState(mockUser(), {
                    ...mockDotRenderedPage(),
                    ...{
                        viewAs: {
                            ...mockDotRenderedPage().viewAs,
                            persona: mockDotPersona
                        }
                    },
                    ...{
                        numberContents: 0
                    }
                });
                service.setLocalState(renderedPage);

                const subscribeCallback = jasmine.createSpy('spy');
                service.haveContent$.subscribe(subscribeCallback);

                expect(subscribeCallback).toHaveBeenCalledWith(false);

                service.updatePageStateHaveContent({
                    type: PageModelChangeEventType.ADD_CONTENT,
                    model: []
                });
                expect(subscribeCallback).toHaveBeenCalledTimes(1);
            });
        });
    });
});
