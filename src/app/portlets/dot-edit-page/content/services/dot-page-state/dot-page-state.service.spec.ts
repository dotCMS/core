import { of, throwError } from 'rxjs';
import { ConnectionBackend } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

import { LoginService, HttpCode } from 'dotcms-js';

import { DOTTestBed } from '@tests/dot-test-bed';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageStateService } from './dot-page-state.service';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DotPageMode } from '@portlets/dot-edit-page/shared/models/dot-page-mode.enum';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { LoginServiceMock } from '@tests/login-service.mock';
import { mockDotRenderedPage } from '@tests/dot-page-render.mock';
import { dotcmsContentletMock } from '@tests/dotcms-contentlet.mock';
import { mockUser } from '@tests/login-service.mock';
import * as _ from 'lodash';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { DotDevice } from '@shared/models/dot-device/dot-device.model';
import { DotPersona } from '@shared/models/dot-persona/dot-persona.model';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { mockResponseView } from '@tests/response-view.mock';
import { TestBedStatic } from '@angular/core/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { PageModelChangeEventType } from '../dot-edit-content-html/models';

const getDotPageRenderStateMock = () => {
    return new DotPageRenderState(mockUser, mockDotRenderedPage);
};

describe('DotPageStateService', () => {
    let backend: MockBackend;
    let dotContentletLockerService: DotContentletLockerService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotHttpErrorManagerServiceHandle: jasmine.Spy;
    let dotPageRenderService: DotPageRenderService;
    let dotPageRenderServiceGetSpy: jasmine.Spy;
    let dotRouterService: DotRouterService;
    let injector: TestBedStatic;
    let lastConnection;
    let loginService: LoginService;
    let service: DotPageStateService;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [
                DotContentletLockerService,
                DotHttpErrorManagerService,
                DotPageRenderService,
                DotPageStateService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });

        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });

        dotContentletLockerService = injector.get(DotContentletLockerService);
        dotHttpErrorManagerService = injector.get(DotHttpErrorManagerService);
        dotPageRenderService = injector.get(DotPageRenderService);
        dotRouterService = injector.get(DotRouterService);
        loginService = injector.get(LoginService);

        dotPageRenderServiceGetSpy = spyOn(dotPageRenderService, 'get').and.returnValue(
            of(mockDotRenderedPage)
        );

        dotHttpErrorManagerServiceHandle = spyOn(
            dotHttpErrorManagerService,
            'handle'
        ).and.callThrough();

        service = injector.get(DotPageStateService);

        spyOnProperty(dotRouterService, 'queryParams', 'get').and.returnValue({
            url: '/an/url/test/form/query/params'
        });
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
                    cssWidth: ''
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
                expect(dotRouterService.replaceQueryParams).toHaveBeenCalledWith({language_id: 1});
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
                .subscribe(() => {});

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
                .subscribe(() => {});

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
                .subscribe(() => {});

            expect(service.reload).toHaveBeenCalledTimes(1);
            expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        });
    });

    describe('internal navigation state', () => {
        it('should return content from setted internal state', () => {
            const renderedPage = getDotPageRenderStateMock();
            service.setInternalNavigationState(renderedPage);

            expect(service.getInternalNavigationState()).toEqual(renderedPage);
            expect(dotPageRenderServiceGetSpy).not.toHaveBeenCalled();
        });

        it('should return null when internal state is not set', () => {
            expect(service.getInternalNavigationState()).toEqual(null);
            expect(dotPageRenderServiceGetSpy).not.toHaveBeenCalled();
        });
    });

    describe('setting local state', () => {
        it('should set local state and emit', () => {
            const renderedPage = getDotPageRenderStateMock();

            service.state$.subscribe((state: DotPageRenderState) => {
                expect(state).toEqual(renderedPage);
            });

            service.setLocalState(renderedPage);
        });
    });

    describe('login as user', () => {
        beforeEach(() => {
            spyOnProperty(loginService, 'auth', 'get').and.returnValue({
                loginAsUser: {
                    userId: 'login-as-user'
                },
                user: {
                    userId: '123'
                }
            });
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
                const renderedPage = getDotPageRenderStateMock();
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
                const renderedPage = new DotPageRenderState(mockUser, {
                    ...mockDotRenderedPage,
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
                const renderedPage = new DotPageRenderState(mockUser, {
                    ...mockDotRenderedPage,
                    ...{
                        viewAs: {
                            ...mockDotRenderedPage.viewAs,
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
                const renderedPage = new DotPageRenderState(mockUser, {
                    ...mockDotRenderedPage,
                    ...{
                        viewAs: {
                            ...mockDotRenderedPage.viewAs,
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
