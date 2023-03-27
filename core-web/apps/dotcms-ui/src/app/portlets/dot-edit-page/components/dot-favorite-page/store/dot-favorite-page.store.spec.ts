import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { mockDotCMSTempFile } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.component.spec';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { CurrentUserDataMock } from '@dotcms/app/portlets/dot-starter/dot-starter-resolver.service.spec';
import { MockDotHttpErrorManagerService } from '@dotcms/app/test/dot-http-error-manager.service.mock';
import {
    DotContentletService,
    DotCurrentUserService,
    DotMessageService,
    DotPageRenderService,
    DotRolesService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCurrentUser, DotRole } from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    MockDotMessageService,
    mockDotRenderedPage,
    mockProcessedRoles
} from '@dotcms/utils-testing';

import { DotFavoritePageActionState, DotFavoritePageStore } from './dot-favorite-page.store';

@Injectable()
class MockDotRolesService {
    public search(): Observable<DotRole[]> {
        return of(mockProcessedRoles);
    }
}

@Injectable()
class MockDotCurrentUserService {
    public getCurrentUser(): Observable<DotCurrentUser> {
        return of(CurrentUserDataMock);
    }
}

@Injectable()
class MockDotTempFileUploadService {
    public upload(): Observable<unknown> {
        return null;
    }
}

@Injectable()
class MockDotWorkflowActionsFireService {
    public publishContentletAndWaitForIndex(): Observable<unknown> {
        return null;
    }
    public deleteContentlet(): Observable<unknown> {
        return null;
    }
}

const messageServiceMock = new MockDotMessageService({
    'favoritePage.dialog.error.tmpFile.upload': 'Upload Error'
});

describe('DotFavoritePageStore', () => {
    let dotFavoritePageStore: DotFavoritePageStore;
    let dotRolesService: DotRolesService;
    let dotCurrentUser: DotCurrentUserService;
    let dotPageRenderService: DotPageRenderService;
    let dotContentletService: DotContentletService;
    let dotTempFileUploadService: DotTempFileUploadService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotFavoritePageStore,
                DotContentletService,
                DotPageRenderService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotCurrentUserService, useClass: MockDotCurrentUserService },
                { provide: DotRolesService, useClass: MockDotRolesService },

                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: DotHttpErrorManagerService, useClass: MockDotHttpErrorManagerService },
                { provide: DotTempFileUploadService, useClass: MockDotTempFileUploadService },
                {
                    provide: DotWorkflowActionsFireService,
                    useClass: MockDotWorkflowActionsFireService
                }
            ]
        });
        dotFavoritePageStore = TestBed.inject(DotFavoritePageStore);
        dotRolesService = TestBed.inject(DotRolesService);
        dotCurrentUser = TestBed.inject(DotCurrentUserService);
        dotPageRenderService = TestBed.inject(DotPageRenderService);
        dotContentletService = TestBed.inject(DotContentletService);
        dotTempFileUploadService = TestBed.inject(DotTempFileUploadService);
        dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);

        spyOn(dotRolesService, 'search').and.callThrough();
        spyOn(dotCurrentUser, 'getCurrentUser').and.callThrough();
        spyOn(dotPageRenderService, 'get').and.returnValue(of(mockDotRenderedPage()));
        spyOn(dotContentletService, 'getContentletPermissions').and.returnValue(
            of({ READ: ['a1', 'b1'] })
        );
    });

    describe('New Favorite Page', () => {
        beforeEach(() => {
            dotFavoritePageStore.setInitialStateData({
                favoritePageUrl: ''
            });
        });

        it('should set initial data', (done) => {
            const expectedInitialState = {
                roleOptions: mockProcessedRoles,
                formState: {
                    currentUserRoleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
                    inode: '',
                    order: 1,
                    permissions: [],
                    thumbnail: '',
                    title: 'A title',
                    url: ''
                },
                isAdmin: true,
                imgWidth: 1024,
                imgHeight: 768.192048012003,
                renderThumbnail: true,
                loading: false,
                pageRenderedHtml: '<html><head></header><body><p>Hello World</p></body></html>',
                showFavoriteEmptySkeleton: undefined,
                closeDialog: false,
                actionState: null
            };

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state).toEqual(expectedInitialState);
                done();
            });
            expect(dotRolesService.search).toHaveBeenCalledTimes(1);
            expect(dotCurrentUser.getCurrentUser).toHaveBeenCalledTimes(1);
            expect(dotPageRenderService.get).toHaveBeenCalledTimes(1);
        });

        // Updaters
        it('should update setRenderThumbnail flag', () => {
            dotFavoritePageStore.setRenderThumbnail(true);
            dotFavoritePageStore.state$.subscribe((data) => {
                expect(data.renderThumbnail).toEqual(true);
            });
        });

        it('should update setShowFavoriteEmptySkeleton flag', () => {
            dotFavoritePageStore.setShowFavoriteEmptySkeleton(true);
            dotFavoritePageStore.state$.subscribe((data) => {
                expect(data.showFavoriteEmptySkeleton).toEqual(true);
            });
        });

        // Selectors
        it('should have actionState$ Selector', () => {
            dotFavoritePageStore.actionState$.subscribe((data) => {
                expect(data).toEqual(null);
            });
        });

        it('should have renderThumbnail$ Selector', () => {
            dotFavoritePageStore.renderThumbnail$.subscribe((data) => {
                expect(data).toEqual(true);
            });
        });

        it('should have closeDialog$ Selector', () => {
            dotFavoritePageStore.closeDialog$.subscribe((data) => {
                expect(data).toEqual(false);
            });
        });

        it('should have form data Selector', () => {
            dotFavoritePageStore.formState$.subscribe((data) => {
                expect(data).toEqual({
                    currentUserRoleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
                    inode: '',
                    order: 1,
                    permissions: [],
                    thumbnail: '',
                    title: 'A title',
                    url: ''
                });
            });
        });

        // Effects
        it('should create a Favorite Page with thumbnail', (done) => {
            spyOn(dotTempFileUploadService, 'upload').and.returnValue(of([mockDotCMSTempFile]));
            spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).and.returnValue(of(null));

            const file = new File(
                [
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC'
                ],
                'image.png'
            );

            dotFavoritePageStore.saveFavoritePage({
                currentUserRoleId: CurrentUserDataMock.roleId,
                thumbnail:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1,
                permissions: []
            });

            expect(dotTempFileUploadService.upload).toHaveBeenCalledWith(file);

            expect(
                dotWorkflowActionsFireService.publishContentletAndWaitForIndex
            ).toHaveBeenCalledWith(
                'dotFavoritePage',
                {
                    screenshot: 'temp-file_123',
                    inode: null,
                    title: 'A title',
                    url: '/an/url/test?language_id=1',
                    order: 1
                },
                { READ: [CurrentUserDataMock.roleId, '6b1fa42f-8729-4625-80d1-17e4ef691ce7'] }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
                done();
            });
        });

        it('should create a Favorite Page without thumbnail', (done) => {
            spyOn(dotTempFileUploadService, 'upload').and.returnValue(of([mockDotCMSTempFile]));
            spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).and.returnValue(of(null));

            dotFavoritePageStore.saveFavoritePage({
                currentUserRoleId: CurrentUserDataMock.roleId,
                thumbnail: '',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1,
                permissions: []
            });

            expect(dotTempFileUploadService.upload).toHaveBeenCalledTimes(0);

            expect(
                dotWorkflowActionsFireService.publishContentletAndWaitForIndex
            ).toHaveBeenCalledWith(
                'dotFavoritePage',
                {
                    screenshot: '',
                    inode: null,
                    title: 'A title',
                    url: '/an/url/test?language_id=1',
                    order: 1
                },
                { READ: [CurrentUserDataMock.roleId, '6b1fa42f-8729-4625-80d1-17e4ef691ce7'] }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
                done();
            });
        });

        it('should Edit a Favorite Page', (done) => {
            spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).and.returnValue(of(null));

            dotFavoritePageStore.saveFavoritePage({
                currentUserRoleId: CurrentUserDataMock.roleId,
                inode: 'abc123',
                thumbnail:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1,
                permissions: []
            });

            expect(
                dotWorkflowActionsFireService.publishContentletAndWaitForIndex
            ).toHaveBeenCalledWith(
                'dotFavoritePage',
                {
                    screenshot:
                        'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                    inode: 'abc123',
                    title: 'A title',
                    url: '/an/url/test?language_id=1',
                    order: 1
                },
                { READ: [CurrentUserDataMock.roleId, '6b1fa42f-8729-4625-80d1-17e4ef691ce7'] }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
                done();
            });
        });

        it('should handle error when create/save Favorite Page', (done) => {
            spyOn(dotTempFileUploadService, 'upload').and.returnValue(of([mockDotCMSTempFile]));
            spyOn(dotWorkflowActionsFireService, 'publishContentletAndWaitForIndex').and.throwError(
                'error'
            );
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();

            dotFavoritePageStore.saveFavoritePage({
                currentUserRoleId: CurrentUserDataMock.roleId,
                thumbnail:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1,
                permissions: []
            });

            expect(
                dotWorkflowActionsFireService.publishContentletAndWaitForIndex
            ).toHaveBeenCalledWith(
                'dotFavoritePage',
                {
                    screenshot: 'temp-file_123',
                    inode: null,
                    title: 'A title',
                    url: '/an/url/test?language_id=1',
                    order: 1
                },
                { READ: [CurrentUserDataMock.roleId, '6b1fa42f-8729-4625-80d1-17e4ef691ce7'] }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
                expect(state.closeDialog).toEqual(false);
                done();
            });
        });

        it('should delete Favorite Page', (done) => {
            spyOn(dotWorkflowActionsFireService, 'deleteContentlet').and.returnValue(of(null));

            dotFavoritePageStore.deleteFavoritePage('abc123');

            expect(dotWorkflowActionsFireService.deleteContentlet).toHaveBeenCalledWith({
                inode: 'abc123'
            });

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.DELETED);
                done();
            });
        });

        it('should handle error when delete Favorite Page', (done) => {
            spyOn(dotWorkflowActionsFireService, 'deleteContentlet').and.throwError('error');
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();

            dotFavoritePageStore.deleteFavoritePage('abc123');

            expect(dotWorkflowActionsFireService.deleteContentlet).toHaveBeenCalledWith({
                inode: 'abc123'
            });

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
                expect(state.closeDialog).toEqual(false);
                expect(state.loading).toEqual(false);
                done();
            });
        });
    });

    describe('Existing Favorite Page', () => {
        const existingDataMock = {
            ...dotcmsContentletMock,
            identifier: 'abc123',
            title: 'preview1',
            screenshot: 'test1',
            url: '/index1?host_id=A&language_id=1&device_inode=123',
            order: 1,
            owner: 'admin'
        };

        beforeEach(() => {
            dotFavoritePageStore.setInitialStateData({
                favoritePageUrl: existingDataMock.url,
                favoritePage: { ...existingDataMock }
            });
        });

        it('should set initial data', (done) => {
            const expectedInitialState = {
                roleOptions: mockProcessedRoles,
                formState: {
                    currentUserRoleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
                    inode: '',
                    order: 1,
                    permissions: ['a1', 'b1'],
                    thumbnail: existingDataMock.screenshot,
                    title: existingDataMock.title,
                    url: existingDataMock.url
                },
                isAdmin: true,
                imgWidth: 1024,
                imgHeight: 768.192048012003,
                renderThumbnail: false,
                loading: false,
                pageRenderedHtml: '<html><head></header><body><p>Hello World</p></body></html>',
                showFavoriteEmptySkeleton: false,
                closeDialog: false,
                actionState: null
            };

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state).toEqual(expectedInitialState);
                done();
            });
            expect(dotRolesService.search).toHaveBeenCalledTimes(1);
            expect(dotCurrentUser.getCurrentUser).toHaveBeenCalledTimes(1);
            expect(dotPageRenderService.get).toHaveBeenCalledTimes(1);
        });
    });
});
