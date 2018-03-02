import { LoginServiceMock } from '../../../test/login-service.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../dot-router/dot-router.service';
import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, Response, ResponseOptions } from '@angular/http';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { EditPageService } from './edit-page.service';
import { DotRenderedPage } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { DotEditPageState } from '../../../shared/models/dot-edit-page-state/dot-edit-page-state.model';
import { DotRenderedPageState } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { RouterTestingModule } from '@angular/router/testing';
import { PageMode } from '../../../portlets/dot-edit-page/content/shared/page-mode.enum';

const mockDotRenderPage: DotRenderedPage = {
    canEdit: true,
    canLock: true,
    identifier: '',
    languageId: 1,
    liveInode: '',
    lockMessage: '',
    locked: true,
    lockedBy: 'someone',
    lockedByAnotherUser: true,
    lockedByName: 'Some One',
    lockedOn: new Date(1517330917295),
    mode: PageMode.PREVIEW,
    pageURI: '',
    render: '<html><</html>',
    shortyLive: '',
    shortyWorking: '',
    title: '',
    workingInode: ''
};

describe('EditPageService', () => {
    let editPageService: EditPageService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [EditPageService, DotRouterService, {
                provide: LoginService,
                useClass: LoginServiceMock
            }],
            imports: [RouterTestingModule]
        });

        editPageService = injector.get(EditPageService);

        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should do a get a rendered page in edit mode', () => {
        let result: DotRenderedPage;
        editPageService.getEdit('about-us').subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockDotRenderPage
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/about-us?mode=EDIT_MODE');
        expect(result).toEqual(mockDotRenderPage);
    });

    it('should do a get a rendered page in preview mode', () => {
        let result: DotRenderedPage;
        editPageService.getPreview('about-us').subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockDotRenderPage
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/about-us?mode=PREVIEW_MODE');
        expect(result).toEqual(mockDotRenderPage);
    });

    it('should do a get a rendered page in live mode', () => {
        let result: DotRenderedPage;
        editPageService.getLive('about-us').subscribe((renderedPage: DotRenderedPage) => result = renderedPage);

        lastConnection[0].mockRespond(new Response(new ResponseOptions({
            body: mockDotRenderPage
        })));
        expect(lastConnection[0].request.url).toContain('/api/v1/page/renderHTML/about-us?mode=LIVE');
        expect(result).toEqual(mockDotRenderPage);
    });

    it('should lock a content asset', () => {
        let result: any;
        editPageService.lock('123').subscribe((lockInfo: any) => (result = lockInfo));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        message: 'locked'
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/content/lock/inode/123');
        expect(result).toEqual({ message: 'locked' });
    });

    it('should unlock a content asset', () => {
        let result: any;
        editPageService.unlock('123').subscribe((lockInfo: any) => (result = lockInfo));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        message: 'locked'
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/content/unlock/inode/123');
        expect(result).toEqual({ message: 'locked' });
    });

    it('should set a page state (lock and the get edit)', () => {
        spyOn(editPageService, 'getEdit').and.callThrough();
        spyOn(editPageService, 'getLive');
        spyOn(editPageService, 'getPreview');
        spyOn(editPageService, 'lock').and.callThrough();
        spyOn(editPageService, 'unlock');

        const state: DotEditPageState = {
            locked: true,
            mode: PageMode.EDIT
        };

        let result: DotRenderedPageState;
        editPageService.setPageState(mockDotRenderPage, state).subscribe((res) => {
            result = res;
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
                    body: mockDotRenderPage
                })
            )
        );

        expect(editPageService.lock).toHaveBeenCalledTimes(1);
        expect(editPageService.getEdit).toHaveBeenCalledTimes(1);
        expect(result).toEqual({
            dotRenderedPage: {
                ...mockDotRenderPage,
                locked: true
            },
            lockState: 'locked'
        });
        expect(editPageService.getLive).not.toHaveBeenCalled();
        expect(editPageService.getPreview).not.toHaveBeenCalled();
        expect(editPageService.unlock).not.toHaveBeenCalled();
    });

    it('should set a page state (just lock)', () => {
        spyOn(editPageService, 'getEdit');
        spyOn(editPageService, 'getLive');
        spyOn(editPageService, 'getPreview');
        spyOn(editPageService, 'lock').and.callThrough();
        spyOn(editPageService, 'unlock');

        const state: DotEditPageState = {
            locked: true
        };

        let result: DotRenderedPageState;
        editPageService.setPageState(mockDotRenderPage, state).subscribe((res) => {
            result = res;
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

        expect(editPageService.lock).toHaveBeenCalledTimes(1);
        expect(result).toEqual({
            lockState: 'locked'
        });

        expect(editPageService.getEdit).not.toHaveBeenCalled();
        expect(editPageService.getLive).not.toHaveBeenCalled();
        expect(editPageService.getPreview).not.toHaveBeenCalled();
        expect(editPageService.unlock).not.toHaveBeenCalled();
    });

    it('should set a page state (just get live)', () => {
        spyOn(editPageService, 'unlock');
        spyOn(editPageService, 'lock');
        spyOn(editPageService, 'getPreview');
        spyOn(editPageService, 'getLive').and.callThrough();
        spyOn(editPageService, 'getEdit');

        const state: DotEditPageState = {
            mode: PageMode.LIVE
        };

        let result: DotRenderedPageState;
        editPageService.setPageState(mockDotRenderPage, state).subscribe((res) => {
            result = res;
        });

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockDotRenderPage
                })
            )
        );
        expect(result).toEqual({
            dotRenderedPage: mockDotRenderPage
        }, 'here');
        expect(editPageService.getLive).toHaveBeenCalledTimes(1);
        expect(editPageService.unlock).not.toHaveBeenCalled();
        expect(editPageService.lock).not.toHaveBeenCalled();
        expect(editPageService.getPreview).not.toHaveBeenCalled();
        expect(editPageService.getEdit).not.toHaveBeenCalled();
    });

    describe('extra custom properties', () => {
        let result: DotRenderedPage;

        const mockResponse = (extras, remove) => {
            const mockedData = {
                ...mockDotRenderPage,
                ...extras
            };

            if (remove) {
                delete mockedData[remove];
            }

            lastConnection[0].mockRespond(new Response(new ResponseOptions({
                body: mockedData
            })));

            return mockedData;
        };

        beforeEach(() => {
            editPageService.getPreview('about-us').subscribe((renderedPage: DotRenderedPage) => {
                result = renderedPage;
            });
        });

        describe('mode', () => {
            it('should set page mode in preview because is locked by another user', () => {
                const mockedData = mockResponse({
                    lockedBy: 'another-user',
                    canLock: true
                }, false);

                expect(result).toEqual({
                    ...mockedData,
                    lockedByAnotherUser: true,
                    locked: true,
                    mode: PageMode.PREVIEW
                });
            });

            it('should set page mode in preview because is locked and user can\'t lock', () => {
                const mockedData = mockResponse({
                    lockedBy: '123',
                    canLock: false
                }, false);

                expect(result).toEqual({
                    ...mockedData,
                    lockedByAnotherUser: false,
                    locked: true,
                    mode: PageMode.PREVIEW
                });
            });

            it('should set page mode in edit because is locked and user can lock and page is locked', () => {
                const mockedData = mockResponse({
                    lockedBy: '123',
                    locked: true,
                    canLock: true
                }, false);

                expect(result).toEqual({
                    ...mockedData,
                    lockedByAnotherUser: false,
                    locked: true,
                    mode: PageMode.EDIT
                });
            });
        });

        describe('locked and locked by another user', () => {
            it('should set object locked by another user and locked', () => {
                const mockedData = mockResponse({
                    lockedBy: 'another-user',
                    canLock: false
                }, false);
                expect(result).toEqual({
                    ...mockedData,
                    lockedByAnotherUser: true,
                    locked: true
                });
            });

            it('should set object NOT locked by another user and unlocked', () => {
                const mockedData = mockResponse({
                    canLock: true
                }, 'lockedBy');

                expect(result).toEqual({
                    ...mockedData,
                    lockedByAnotherUser: false,
                    locked: false
                });
            });

            it('should set page locked by another user and unlocked', () => {
                const mockedData = mockResponse({
                    lockedBy: 'another-user',
                    canLock: true
                }, false);
                expect(result).toEqual({
                    ...mockedData,
                    lockedByAnotherUser: true,
                    locked: true
                });
            });
        });
    });
});
