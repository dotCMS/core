import { describe, it, expect } from '@jest/globals';
import { of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotContentletLockerService,
    DotESContentService,
    DotFavoritePageService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotPageRenderService,
    DotRouterService,
    DotSessionStorageService,
    DotFormatDateService,
    DotExperimentsService
} from '@dotcms/data-access';
import { CoreWebService, HttpCode, LoginService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotDevice,
    DotExperimentStatus,
    DotPageMode,
    DotPageRenderState,
    DotPersona,
    PageModelChangeEventType
} from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    DotLicenseServiceMock,
    DotMessageDisplayServiceMock,
    getExperimentMock,
    LoginServiceMock,
    mockDotPersona,
    mockDotRenderedPage,
    MockDotRouterJestService,
    mockResponseView,
    mockUser,
    mockUserAuth
} from '@dotcms/utils-testing';

import { DotPageStateService } from './dot-page-state.service';

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
    let dotHttpErrorManagerServiceHandle: jest.SpyInstance;
    let dotPageRenderService: DotPageRenderService;
    let dotPageRenderServiceGetSpy: jest.SpyInstance;
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
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useValue: new MockDotRouterJestService(jest) },
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
        service = injector.inject(DotPageStateService);
        dotContentletLockerService = injector.inject(DotContentletLockerService);
        dotHttpErrorManagerService = injector.inject(DotHttpErrorManagerService);
        dotPageRenderService = injector.inject(DotPageRenderService);
        dotRouterService = injector.inject(DotRouterService);
        loginService = injector.inject(LoginService);
        dotFavoritePageService = injector.inject(DotFavoritePageService);
        dotExperimentsService = injector.inject(DotExperimentsService);

        dotPageRenderServiceGetSpy = jest
            .spyOn(dotPageRenderService, 'get')
            .mockReturnValue(of(mockDotRenderedPage()));

        dotHttpErrorManagerServiceHandle = jest.spyOn(dotHttpErrorManagerService, 'handle');

        service = injector.get(DotPageStateService);

        jest.spyOn(dotRouterService, 'queryParams', 'get').mockReturnValue({
            url: '/an/url/test/form/query/params'
        });

        jest.spyOn(dotFavoritePageService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: []
                },
                queryTook: 1,
                resultsSize: 20
            })
        );

        jest.spyOn(dotExperimentsService, 'getByStatus').mockReturnValue(of([]));
    });

    describe('Method: get', () => {
        it('should get with url', () => {
            service.get({
                url: 'some/url/test'
            });

            expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                {
                    url: 'some/url/test'
                },
                {}
            );
        });

        it('should get with url from queryParams', () => {
            service.get();

            expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                {
                    url: '/an/url/test/form/query/params'
                },
                {}
            );

            expect(dotFavoritePageService.get).toHaveBeenCalledWith({
                limit: 10,
                userId: 'dotcms.org.1',
                url: '/an/url/test?&language_id=1&device_inode='
            });
        });

        it('should get with url from queryParams with a Failing fetch from ES Search (favorite page)', () => {
            const error500 = mockResponseView(500, '/test', null, {
                message: 'error'
            });
            dotFavoritePageService.get = jest.fn().mockReturnValue(throwError(error500));
            service.get();

            const subscribeCallback = jest.fn();
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
            dotExperimentsService.getByStatus = jest.fn().mockReturnValue(of([EXPERIMENT_MOCK]));

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

            dotExperimentsService.getByStatus = jest.fn().mockReturnValue(throwError(error500));

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
            expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                {
                    mode: 'PREVIEW_MODE',
                    url: '/an/url/test/form/query/params'
                },
                {}
            );
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
                jest.spyOn(dotContentletLockerService, 'lock').mockReturnValue(
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
                expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                    {
                        mode: 'ADMIN_MODE',
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                );
            });

            it('should unlock', () => {
                jest.spyOn(dotContentletLockerService, 'unlock').mockReturnValue(
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
                expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                    {
                        mode: 'PREVIEW_MODE',
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                );
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

                expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                    {
                        viewAs: {
                            device: device
                        },
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                );
            });
        });

        describe('setLanguage', () => {
            it('should set language 1', () => {
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

                expect(dotPageRenderServiceGetSpy).toHaveBeenCalledWith(
                    {
                        viewAs: {
                            persona: persona
                        },
                        url: '/an/url/test/form/query/params'
                    },
                    {}
                );
            });
        });
    });

    describe('errors', () => {
        it('should show error 300 message and redirect to site browser', () => {
            const error300 = mockResponseView(300);
            dotPageRenderServiceGetSpy.mockReturnValue(throwError(error300));
            dotHttpErrorManagerServiceHandle.mockReturnValue(
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
            dotPageRenderServiceGetSpy.mockReturnValue(throwError(error404));
            dotHttpErrorManagerServiceHandle.mockReturnValue(
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
            jest.spyOn(service, 'reload');
            const error500 = mockResponseView(500);
            dotPageRenderServiceGetSpy.mockReturnValue(throwError(error500));
            dotHttpErrorManagerServiceHandle.mockReturnValue(
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

        it('should set local state and emit with experiment', () => {
            const mock = getDotPageRenderStateMock(dotcmsContentletMock, EXPERIMENT_MOCK);
            dotExperimentsService.getByStatus = jest.fn().mockReturnValue(of([EXPERIMENT_MOCK]));

            const renderedPage = getDotPageRenderStateMock(dotcmsContentletMock);

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(mock);
            });

            service.setLocalState(renderedPage);
        });
    });

    describe('login as user', () => {
        beforeEach(() => {
            jest.spyOn(loginService, 'auth', 'get').mockReturnValue(mockUserAuth);
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
            it('should trigger haveContent as true', () => {
                const renderedPage = getDotPageRenderStateMock(dotcmsContentletMock);
                service.setLocalState(renderedPage);

                const subscribeCallback = jest.fn();
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

                const subscribeCallback = jest.fn();
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

                const subscribeCallback = jest.fn();
                service.haveContent$.subscribe(subscribeCallback);

                expect(subscribeCallback).toHaveBeenCalledWith(true);

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

                const subscribeCallback = jest.fn();
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
