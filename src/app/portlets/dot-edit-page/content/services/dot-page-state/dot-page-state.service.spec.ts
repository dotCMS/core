import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletLockerService } from '../../../../../api/services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageStateService } from './dot-page-state.service';
import { DotRenderHTMLService } from '../../../../../api/services/dot-render-html/dot-render-html.service';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { PageMode } from '../../../shared/models/page-mode.enum';
import { mockDotRenderedPage, mockDotPage } from '../../../../../test/dot-rendered-page.mock';
import * as _ from 'lodash';

describe('DotPageStateService', () => {
    let service: DotPageStateService;
    let loginService: LoginService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [
                DotPageStateService,
                DotRenderHTMLService,
                DotContentletLockerService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });

        service = injector.get(DotPageStateService);
        backend = injector.get(ConnectionBackend) as MockBackend;
        loginService = injector.get(LoginService);
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    describe('set page state', () => {
        it('should set a page locked and live mode', () => {
            service
                .set(mockDotPage, {
                    mode: PageMode.LIVE,
                    locked: true
                })
                .subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.page).toEqual(mockDotPage);
                    expect(updatedPageState.state).toEqual({
                        locked: true,
                        lockedByAnotherUser: true,
                        mode: PageMode.LIVE
                    });
                });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            message: 'locked'
                        }
                    })
                )
            );

            lastConnection[1].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPage
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/content/lock/inode/999');
            expect(lastConnection[1].request.url).toContain('/api/v1/page/renderHTML/an/url/test?mode=ADMIN_MODE');
        });

        it('should set a page unlocked and preview mode', () => {
            const mockDotRenderedPageTest = _.cloneDeep(mockDotRenderedPage);
            mockDotRenderedPageTest.page.lockedBy = null;

            service
                .set(mockDotPage, {
                    mode: PageMode.PREVIEW,
                    locked: false
                })
                .subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.page).toEqual(mockDotRenderedPageTest.page);
                    expect(updatedPageState.state).toEqual({
                        locked: false,
                        lockedByAnotherUser: false,
                        mode: PageMode.PREVIEW
                    });
                });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            message: 'locked'
                        }
                    })
                )
            );

            lastConnection[1].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPageTest
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/content/unlock/inode/999');
            expect(lastConnection[1].request.url).toContain('/api/v1/page/renderHTML/an/url/test?mode=PREVIEW');
        });

        it('should set a page preview mode and keep the lock', () => {
            service
                .set(mockDotPage, {
                    mode: PageMode.PREVIEW
                })
                .subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.page).toEqual(mockDotPage);
                    expect(updatedPageState.state).toEqual({
                        locked: true,
                        lockedByAnotherUser: true,
                        mode: PageMode.PREVIEW
                    });
                });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPage
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/an/url/test?mode=PREVIEW');
            expect(lastConnection[1]).toBeUndefined();
        });
    });

    describe('get a page state', () => {
        it('should get a unlocked page and set default state', () => {
            const { lockedBy, lockMessage, lockedByName, lockedOn, ...noLockedByPage } = mockDotPage;

            service.get('/hello/world').subscribe((updatedPageState: DotRenderedPageState) => {
                expect(updatedPageState.page).toEqual(noLockedByPage);
                expect(updatedPageState.state).toEqual({
                    locked: false,
                    mode: PageMode.PREVIEW,
                    lockedByAnotherUser: false
                });
            });
            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            page: noLockedByPage
                        }
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/hello/world?mode=EDIT_MODE');
        });

        describe('locked by another user', () => {
            beforeEach(() => {
                spyOnProperty(loginService, 'auth', 'get').and.returnValue({
                    loginAsUser: {
                        userId: 'someone'
                    }
                });
            });

            it('should get a locked page and set default state', () => {
                service.get('/test/123').subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.page).toEqual(mockDotPage);
                    expect(updatedPageState.state).toEqual({
                        locked: true,
                        mode: PageMode.EDIT,
                        lockedByAnotherUser: false
                    });
                });

                lastConnection[0].mockRespond(
                    new Response(
                        new ResponseOptions({
                            body: mockDotRenderedPage
                        })
                    )
                );
                expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/test/123?mode=EDIT_MODE');
            });

            it('should get a locked page and set default state locked by another user', () => {
                service.get('/hola/mundo').subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.page).toEqual(mockDotPage);
                    expect(updatedPageState.state).toEqual({
                        locked: true,
                        mode: PageMode.EDIT,
                        lockedByAnotherUser: false
                    });
                });

                lastConnection[0].mockRespond(
                    new Response(
                        new ResponseOptions({
                            body: mockDotRenderedPage
                        })
                    )
                );
                expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/hola/mundo?mode=EDIT_MODE');
            });
        });
    });

    describe('normal user', () => {
        it('should get', () => {
            service.get('/test/123').subscribe((updatedPageState: DotRenderedPageState) => {
                expect(updatedPageState.user.userId).toEqual('123');
            });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPage
                    })
                )
            );
        });

        it('should set', () => {
            service
                .set(mockDotPage, {
                    mode: PageMode.PREVIEW
                })
                .subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.user.userId).toEqual('123');
                });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPage
                    })
                )
            );
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

        it('should get', () => {
            service.get('/test/123').subscribe((updatedPageState: DotRenderedPageState) => {
                expect(updatedPageState.user.userId).toEqual('login-as-user');
            });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPage
                    })
                )
            );
        });

        it('should set', () => {
            service
                .set(mockDotPage, {
                    mode: PageMode.PREVIEW
                })
                .subscribe((updatedPageState: DotRenderedPageState) => {
                    expect(updatedPageState.user.userId).toEqual('login-as-user');
                });

            lastConnection[0].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockDotRenderedPage
                    })
                )
            );
        });
    });
});
