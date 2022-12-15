import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { mockDotCMSTempFile } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.component.spec';
import { DotCurrentUserService } from '@dotcms/data-access';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessageService } from '@dotcms/data-access';
import { DotRolesService } from '@dotcms/data-access';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { CurrentUserDataMock } from '@dotcms/app/portlets/dot-starter/dot-starter-resolver.service.spec';
import { Observable, of } from 'rxjs';
import { DotFavoritePageActionState, DotFavoritePageStore } from './dot-favorite-page.store';
import { DotRole, DotCurrentUser, DotPageRenderState, DotPageRender } from '@dotcms/dotcms-models';
import {
    MockDotMessageService,
    mockDotRenderedPage,
    mockProcessedRoles,
    mockUser
} from '@dotcms/utils-testing';
import { MockDotHttpErrorManagerService } from '@dotcms/app/test/dot-http-error-manager.service.mock';

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

const mockRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

describe('DotFavoritePageStore', () => {
    let dotFavoritePageStore: DotFavoritePageStore;
    let dotRolesService: DotRolesService;
    let dotCurrentUser: DotCurrentUserService;
    let dotTempFileUploadService: DotTempFileUploadService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotFavoritePageStore,
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
        dotTempFileUploadService = TestBed.inject(DotTempFileUploadService);
        dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);

        spyOn(dotRolesService, 'search').and.callThrough();
        spyOn(dotCurrentUser, 'getCurrentUser').and.callThrough();

        dotFavoritePageStore.setInitialStateData({
            isAdmin: mockRenderedPageState.user.admin,
            imgWidth: mockRenderedPageState.viewAs.device?.cssWidth,
            imgHeight: mockRenderedPageState.viewAs.device?.cssHeight,
            pageRenderedHtml: '<p>test</p>'
        });
    });

    it('should set initial data', (done) => {
        const expectedInitialState = {
            roleOptions: mockProcessedRoles,
            currentUserRoleId: CurrentUserDataMock.roleId,
            isAdmin: true,
            imgWidth: 1024,
            imgHeight: 768.192048012003,
            loading: false,
            pageRenderedHtml: '<p>test</p>',
            closeDialog: false,
            actionState: null
        };

        dotFavoritePageStore.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
        expect(dotRolesService.search).toHaveBeenCalledTimes(1);
        expect(dotCurrentUser.getCurrentUser).toHaveBeenCalledTimes(1);
    });

    // Updaters
    it('should update Favorite Pages', () => {
        dotFavoritePageStore.setInodeStored('test');
        dotFavoritePageStore.state$.subscribe((data) => {
            expect(data.inodeStored).toEqual('test');
        });
    });

    // Selectors
    it('should have closeDialog$ Selector', () => {
        dotFavoritePageStore.closeDialog$.subscribe((data) => {
            expect(data).toEqual(false);
        });
    });

    it('should have currentUserRoleId$ Selector', () => {
        dotFavoritePageStore.currentUserRoleId$.subscribe((data) => {
            expect(data).toEqual(CurrentUserDataMock.roleId);
        });
    });

    // Effects
    it('should save Favorite Page', (done) => {
        spyOn(dotTempFileUploadService, 'upload').and.returnValue(of([mockDotCMSTempFile]));
        spyOn(dotWorkflowActionsFireService, 'publishContentletAndWaitForIndex').and.returnValue(
            of(null)
        );

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

        expect(dotWorkflowActionsFireService.publishContentletAndWaitForIndex).toHaveBeenCalledWith(
            'dotFavoritePage',
            {
                screenshot: 'temp-file_123',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1
            },
            { READ: [CurrentUserDataMock.roleId, '6b1fa42f-8729-4625-80d1-17e4ef691ce7'] }
        );

        dotFavoritePageStore.state$.subscribe((state) => {
            expect(state.closeDialog).toEqual(true);
            expect(state.actionState).toEqual(DotFavoritePageActionState.SAVED);
            done();
        });
    });

    it('should handle error when save Favorite Page', (done) => {
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

        expect(dotWorkflowActionsFireService.publishContentletAndWaitForIndex).toHaveBeenCalledWith(
            'dotFavoritePage',
            {
                screenshot: 'temp-file_123',
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
