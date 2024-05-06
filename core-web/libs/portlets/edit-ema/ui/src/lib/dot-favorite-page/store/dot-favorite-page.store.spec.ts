import { describe, expect } from '@jest/globals';
import { Observable, of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import {
    DotMessageService,
    DotPageRenderService,
    DotWorkflowActionsFireService,
    DotSessionStorageService,
    DotHttpErrorManagerService,
    DotTempFileUploadService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    mockDotCMSTempFile,
    MockDotHttpErrorManagerService,
    MockDotMessageService,
    mockDotRenderedPage,
    mockResponseView
} from '@dotcms/utils-testing';

import {
    CMS_OWNER_ROLE_LIST,
    DotFavoritePageActionState,
    DotFavoritePageStore
} from './dot-favorite-page.store';

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
    let dotPageRenderService: DotPageRenderService;
    let dotTempFileUploadService: DotTempFileUploadService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSessionStorageService,
                DotFavoritePageStore,
                DotPageRenderService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },

                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                },
                {
                    provide: DotTempFileUploadService,
                    useClass: MockDotTempFileUploadService
                },
                {
                    provide: DotWorkflowActionsFireService,
                    useClass: MockDotWorkflowActionsFireService
                }
            ]
        });
        dotFavoritePageStore = TestBed.inject(DotFavoritePageStore);
        dotPageRenderService = TestBed.inject(DotPageRenderService);
        dotTempFileUploadService = TestBed.inject(DotTempFileUploadService);
        dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);

        jest.spyOn(dotPageRenderService, 'get').mockReturnValue(of(mockDotRenderedPage()));
    });

    describe('New Favorite Page', () => {
        beforeEach(() => {
            jest.spyOn(dotPageRenderService, 'checkPermission').mockReturnValue(of(true));

            dotFavoritePageStore.setInitialStateData({
                favoritePageUrl: ''
            });
        });

        it('should set initial data for a page with total user access', (done) => {
            const expectedInitialState = {
                formState: {
                    inode: '',
                    order: 1,
                    thumbnail: '',
                    title: 'A title',
                    url: ''
                },
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
                    inode: '',
                    order: 1,
                    thumbnail: '',
                    title: 'A title',
                    url: ''
                });
            });
        });

        // Effects
        it('should create a Favorite Page with thumbnail', (done) => {
            jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValue(
                of([mockDotCMSTempFile])
            );
            jest.spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).mockReturnValue(of(null));

            const file = new File(
                [
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC'
                ],
                'image.png'
            );

            dotFavoritePageStore.saveFavoritePage({
                thumbnail:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1
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
                {
                    READ: CMS_OWNER_ROLE_LIST,
                    WRITE: CMS_OWNER_ROLE_LIST,
                    PUBLISH: CMS_OWNER_ROLE_LIST
                }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
                done();
            });
        });

        it('should create a Favorite Page without thumbnail', (done) => {
            jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValue(
                of([mockDotCMSTempFile])
            );
            jest.spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).mockReturnValue(of(null));

            dotFavoritePageStore.saveFavoritePage({
                thumbnail: '',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1
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
                {
                    READ: CMS_OWNER_ROLE_LIST,
                    WRITE: CMS_OWNER_ROLE_LIST,
                    PUBLISH: CMS_OWNER_ROLE_LIST
                }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
                done();
            });
        });

        it('should Edit a Favorite Page', (done) => {
            jest.spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).mockReturnValue(of(null));

            dotFavoritePageStore.saveFavoritePage({
                inode: 'abc123',
                thumbnail:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1
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
                {
                    READ: CMS_OWNER_ROLE_LIST,
                    WRITE: CMS_OWNER_ROLE_LIST,
                    PUBLISH: CMS_OWNER_ROLE_LIST
                }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state.closeDialog).toEqual(true);
                expect(state.loading).toEqual(false);
                expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
                done();
            });
        });

        it('should handle error when create/save Favorite Page', (done) => {
            jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValue(
                of([mockDotCMSTempFile])
            );
            jest.spyOn(
                dotWorkflowActionsFireService,
                'publishContentletAndWaitForIndex'
            ).mockImplementation(() => {
                throw new Error('error');
            });
            jest.spyOn(dotHttpErrorManagerService, 'handle');

            dotFavoritePageStore.saveFavoritePage({
                thumbnail:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1
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
                {
                    READ: CMS_OWNER_ROLE_LIST,
                    WRITE: CMS_OWNER_ROLE_LIST,
                    PUBLISH: CMS_OWNER_ROLE_LIST
                }
            );

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
                expect(state.closeDialog).toEqual(false);
                done();
            });
        });

        it('should delete Favorite Page', (done) => {
            jest.spyOn(dotWorkflowActionsFireService, 'deleteContentlet').mockReturnValue(of(null));

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
            jest.spyOn(dotWorkflowActionsFireService, 'deleteContentlet').mockImplementation(() => {
                throw new Error('error');
            });
            jest.spyOn(dotHttpErrorManagerService, 'handle');

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

        it('should set initial data', (done) => {
            jest.spyOn(dotPageRenderService, 'checkPermission').mockReturnValue(of(true));
            dotFavoritePageStore.setInitialStateData({
                favoritePageUrl: existingDataMock.url,
                favoritePage: { ...existingDataMock }
            });

            const expectedInitialState = {
                formState: {
                    inode: '',
                    order: 1,
                    thumbnail: existingDataMock.screenshot,
                    title: existingDataMock.title,
                    url: existingDataMock.url
                },
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
            expect(dotPageRenderService.get).toHaveBeenCalledTimes(1);
        });

        it('should set right title if it is urlContentMap', (done) => {
            jest.spyOn(dotPageRenderService, 'checkPermission').mockReturnValue(of(true));

            dotPageRenderService.get = jest.fn().mockReturnValue(
                of({
                    ...mockDotRenderedPage(),
                    urlContentMap: { title: 'test urlContentMap' }
                })
            );

            dotFavoritePageStore.setInitialStateData({
                favoritePageUrl: existingDataMock.url,
                favoritePage: { ...existingDataMock }
            });

            const expectedInitialState = {
                formState: {
                    inode: '',
                    order: 1,
                    thumbnail: existingDataMock.screenshot,
                    title: 'test urlContentMap',
                    url: existingDataMock.url
                },
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
            expect(dotPageRenderService.get).toHaveBeenCalledTimes(1);
        });

        it('should set initial data for an unknown 404 page', (done) => {
            const error404 = mockResponseView(404);
            dotPageRenderService.checkPermission = jest.fn().mockReturnValue(throwError(error404));

            dotFavoritePageStore.setInitialStateData({
                favoritePageUrl: existingDataMock.url,
                favoritePage: { ...existingDataMock }
            });

            const expectedInitialState = {
                formState: {
                    inode: '',
                    order: 1,
                    thumbnail: 'test1',
                    title: 'preview1',
                    url: '/index1?host_id=A&language_id=1&device_inode=123'
                },
                imgWidth: 1024,
                imgHeight: 1.333,
                renderThumbnail: false,
                loading: false,
                pageRenderedHtml: '',
                showFavoriteEmptySkeleton: false,
                closeDialog: false,
                actionState: null
            };

            dotFavoritePageStore.state$.subscribe((state) => {
                expect(state).toEqual(expectedInitialState);
                done();
            });
        });
    });
});
