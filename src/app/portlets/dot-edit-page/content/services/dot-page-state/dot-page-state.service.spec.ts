import { of as observableOf } from 'rxjs';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageStateService } from './dot-page-state.service';
import { DotRenderHTMLService } from '@services/dot-render-html/dot-render-html.service';
import { PageMode } from '@portlets/dot-edit-page/shared/models/page-mode.enum';
import { DotRenderedPageState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { DotRenderedPage } from '@portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { mockDotRenderedPage, mockDotPage } from '../../../../../test/dot-rendered-page.mock';
import { mockUser } from '../../../../../test/login-service.mock';
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

            const mockDotRenderedPageCopy: DotRenderedPage = _.cloneDeep(mockDotRenderedPage);
            mockDotRenderedPageCopy.viewAs.mode = PageMode[PageMode.LIVE];

            lastConnection[1].mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            entity: mockDotRenderedPageCopy
                        }
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/content/lock/inode/999');
            expect(lastConnection[1].request.url).toContain(
                '/api/v1/page/render/an/url/test?mode=ADMIN_MODE'
            );
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
                        body: {
                            entity: mockDotRenderedPageTest
                        }
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/content/unlock/inode/999');
            expect(lastConnection[1].request.url).toContain(
                '/api/v1/page/render/an/url/test?mode=PREVIEW'
            );
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
                        body: {
                            entity: mockDotRenderedPage
                        }
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain(
                '/api/v1/page/render/an/url/test?mode=PREVIEW'
            );
            expect(lastConnection[1]).toBeUndefined();
        });
    });

    describe('reload page state', () => {
        it('should emit reload evt with DotRenderedPageState', () => {
            const renderedPage = new DotRenderedPageState(mockUser, {
                ...mockDotRenderedPage
            });
            spyOn(service, 'get').and.returnValue(observableOf(renderedPage));
            service.reload$.subscribe((page: DotRenderedPageState) => {
                expect(page).toBe(renderedPage);
            });

            service.reload({
                url: '/hello/world'
            });
        });
    });

    describe('get a page state', () => {
        it('should get a unlocked page and set default state', () => {
            const {
                lockedBy,
                lockMessage,
                lockedByName,
                lockedOn,
                ...noLockedByPage
            } = mockDotPage;

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
                            entity: {
                                page: noLockedByPage,
                                viewAs: {
                                    mode: PageMode[PageMode.PREVIEW]
                                }
                            }
                        }
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain('/api/v1/page/render/hello/world');
        });

        it('should get a page in a specific language', () => {
            const {
                lockedBy,
                lockMessage,
                lockedByName,
                lockedOn,
                ...noLockedByPage
            } = mockDotPage;

            const options = {
                url: '/hello/world',
                viewAs: {
                    language_id: 2
                }
            };

            service.get(options).subscribe((updatedPageState: DotRenderedPageState) => {
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
                            entity: {
                                page: noLockedByPage,
                                viewAs: {
                                    mode: PageMode[PageMode.PREVIEW]
                                }
                            }
                        }
                    })
                )
            );

            expect(lastConnection[0].request.url).toContain(
                '/api/v1/page/render/hello/world?language_id=2'
            );
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

                const mockDotRenderedPageCopy: DotRenderedPage = _.cloneDeep(mockDotRenderedPage);
                mockDotRenderedPageCopy.viewAs.mode = PageMode[PageMode.EDIT];

                lastConnection[0].mockRespond(
                    new Response(
                        new ResponseOptions({
                            body: {
                                entity: mockDotRenderedPageCopy
                            }
                        })
                    )
                );
                expect(lastConnection[0].request.url).toContain('/api/v1/page/render/test/123');
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

                const mockDotRenderedPageCopy: DotRenderedPage = _.cloneDeep(mockDotRenderedPage);
                mockDotRenderedPageCopy.viewAs.mode = PageMode[PageMode.EDIT];

                lastConnection[0].mockRespond(
                    new Response(
                        new ResponseOptions({
                            body: {
                                entity: mockDotRenderedPageCopy
                            }
                        })
                    )
                );
                expect(lastConnection[0].request.url).toContain('/api/v1/page/render/hola/mundo');
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
                        body: {
                            entity: mockDotRenderedPage
                        }
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
                        body: {
                            entity: mockDotRenderedPage
                        }
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
                        body: {
                            entity: mockDotRenderedPage
                        }
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
                        body: {
                            entity: mockDotRenderedPage
                        }
                    })
                )
            );
        });
    });
});
