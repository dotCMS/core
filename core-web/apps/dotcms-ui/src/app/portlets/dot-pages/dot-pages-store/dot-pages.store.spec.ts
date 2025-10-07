import { Observable, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotFavoritePageService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLanguagesService,
    DotLicenseService,
    DotLocalstorageService,
    DotMessageDisplayService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotPropertiesService,
    DotRenderMode,
    DotRouterService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    ESOrderDirection,
    PushPublishService
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    SiteService,
    SiteServiceMock,
    StringUtils
} from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    ESContent
} from '@dotcms/dotcms-models';
import {
    createFakeEvent,
    DotcmsConfigServiceMock,
    dotcmsContentletMock,
    dotcmsContentTypeBasicMock,
    DotcmsEventsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotHttpErrorManagerService,
    MockDotRouterService,
    mockLanguageArray,
    mockPublishAction,
    mockResponseView,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import {
    DotPageStore,
    LOCAL_STORAGE_FAVORITES_PANEL_KEY,
    SESSION_STORAGE_FAVORITES_KEY
} from './dot-pages.store';

import { PushPublishServiceMock } from '../../../view/components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.component.spec';
import { contentTypeDataMock } from '../../dot-edit-page/components/dot-palette/dot-palette-content-type/dot-palette-content-type.component.spec';
import {
    CurrentUserDataMock,
    DotCurrentUserServiceMock
} from '../../dot-starter/dot-starter-resolver.service.spec';
import { DotPagesCreatePageDialogComponent } from '../dot-pages-create-page-dialog/dot-pages-create-page-dialog.component';
import { favoritePagesInitialTestData } from '../dot-pages.component.spec';

@Injectable()
class MockESPaginatorService {
    paginationPerPage = 15;
    totalRecords = 20;

    public get(): Observable<ESContent> {
        return of({
            contentTook: 1,
            jsonObjectView: { contentlets: favoritePagesInitialTestData },
            queryTook: 1,
            resultsSize: favoritePagesInitialTestData.length
        });
    }
}

@Injectable()
export class DialogServiceMock {
    open(): void {
        /* */
    }
}

describe('DotPageStore', () => {
    let dotPageStore: DotPageStore;
    let dialogService: DialogService;
    let dotESContentService: DotESContentService;
    let dotPageTypesService: DotPageTypesService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    let dotPageWorkflowsActionsService: DotPageWorkflowsActionsService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotFavoritePageService: DotFavoritePageService;
    let dotLocalstorageService: DotLocalstorageService;
    let dotPropertiesService: DotPropertiesService;
    let dotPushPublishDialogService: DotPushPublishDialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotEventsService,
                DotGlobalMessageService,
                DotIframeService,
                DotPageStore,
                DotPageTypesService,
                DotWizardService,
                DotWorkflowActionsFireService,
                DotWorkflowsActionsService,
                DotPageWorkflowsActionsService,
                DotWorkflowEventHandlerService,
                LoggerService,
                StringUtils,
                DotFavoritePageService,
                DotLocalstorageService,
                DotPropertiesService,
                DotPushPublishDialogService,
                { provide: DialogService, useClass: DialogServiceMock },
                { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: DotcmsConfigService, useClass: DotcmsConfigServiceMock },
                { provide: DotLanguagesService, useClass: DotLanguagesServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                },
                { provide: DotESContentService, useClass: MockESPaginatorService },
                { provide: DotLicenseService, useClass: DotLicenseServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: SiteService, useClass: SiteServiceMock },
                { provide: PushPublishService, useClass: PushPublishServiceMock }
            ]
        });
        dotPageStore = TestBed.inject(DotPageStore);
        dialogService = TestBed.inject(DialogService);
        dotESContentService = TestBed.inject(DotESContentService);
        dotPageTypesService = TestBed.inject(DotPageTypesService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        dotWorkflowsActionsService = TestBed.inject(DotWorkflowsActionsService);
        dotPageWorkflowsActionsService = TestBed.inject(DotPageWorkflowsActionsService);
        dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
        dotFavoritePageService = TestBed.inject(DotFavoritePageService);
        dotLocalstorageService = TestBed.inject(DotLocalstorageService);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);

        jest.spyOn(dialogService, 'open');
        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(dotLocalstorageService, 'getItem').mockReturnValue(`true`);
        jest.spyOn(dotPropertiesService, 'getKey').mockReturnValue(of('*'));
        jest.spyOn(dotPropertiesService, 'getFeatureFlag').mockReturnValue(of(false));

        dotPageStore.setInitialStateData(5);
        dotPageStore.setKeyword('test');
        dotPageStore.setLanguageId('1');
        dotPageStore.setArchived('true');
    });

    it('should load Favorite Pages initial data', () => {
        dotPageStore.state$.subscribe((data) => {
            expect(data.environments).toEqual(true);
            expect(data.favoritePages.items).toEqual(favoritePagesInitialTestData);
            expect(data.favoritePages.showLoadMoreButton).toEqual(false);
            expect(data.favoritePages.total).toEqual(favoritePagesInitialTestData.length);
            expect(data.isEnterprise).toEqual(true);
            expect(data.languages).toEqual(mockLanguageArray);
            expect(data.loggedUser.id).toEqual(CurrentUserDataMock.userId);
            expect(data.loggedUser.canRead).toEqual({
                contentlets: true,
                htmlPages: true
            });
            expect(data.loggedUser.canWrite).toEqual({
                contentlets: true,
                htmlPages: true
            });
            expect(data.pages.items).toEqual([]);
            expect(data.pages.keyword).toEqual('test');
            expect(data.pages.status).toEqual(ComponentStatus.INIT);
        });
    });

    it('should load null Favorite Pages data when error on initial data fetch', () => {
        const error500 = mockResponseView(500, '/test', null, { message: 'error' });
        jest.spyOn(dotESContentService, 'get').mockReturnValue(throwError(error500));
        // Mock sessionStorage.getItem
        (sessionStorage.getItem as jest.Mock).mockReturnValue(null);

        dotPageStore.setInitialStateData(5);
        expect(sessionStorage.getItem).toHaveBeenCalledWith(SESSION_STORAGE_FAVORITES_KEY);
        // sessionStorage.getItem is called multiple times during initialization
        expect(sessionStorage.getItem).toHaveBeenCalledTimes(3);

        dotPageStore.state$.subscribe((data) => {
            expect(data.environments).toEqual(false);
            expect(data.favoritePages.items).toEqual([]);
            expect(data.favoritePages.showLoadMoreButton).toEqual(false);
            expect(data.favoritePages.total).toEqual(0);
            expect(data.isEnterprise).toEqual(false);
            expect(data.languages).toEqual(null);
            expect(data.loggedUser.id).toEqual(null);
            expect(data.loggedUser.canRead).toEqual({
                contentlets: null,
                htmlPages: null
            });
            expect(data.loggedUser.canWrite).toEqual({
                contentlets: null,
                htmlPages: null
            });
            expect(data.pages.items).toEqual([]);
            expect(data.pages.keyword).toEqual('');
            expect(data.pages.status).toEqual(ComponentStatus.INIT);
        });
    });

    // Selectors
    it('should get language options for dropdown', () => {
        dotPageStore.languageOptions$.subscribe((data) => {
            expect(data).toEqual([
                { label: 'All', value: null },
                { label: 'English (US)', value: 1 },
                { label: 'Italian', value: 2 }
            ]);
        });
    });

    it('should get language Labels for row field', () => {
        dotPageStore.languageLabels$.subscribe((data) => {
            expect(data).toEqual({ '1': 'en-US', '2': 'IT' });
        });
    });

    it('should get pages status', () => {
        dotPageStore.getStatus$.subscribe((data) => {
            expect(data).toEqual(ComponentStatus.INIT);
        });
    });

    it('should get keywordValue status', () => {
        dotPageStore.keywordValue$.subscribe((data) => {
            expect(data).toEqual('test');
        });
    });

    it('should get showArchivedValue status', () => {
        dotPageStore.showArchivedValue$.subscribe((data) => {
            expect(data).toEqual(true);
        });
    });

    it('should get languageIdValue status', () => {
        dotPageStore.languageIdValue$.subscribe((data) => {
            expect(data).toEqual(1);
        });
    });

    it('should get pages Filter Params', () => {
        dotPageStore.getFilterParams$.subscribe((data) => {
            expect(data).toEqual({
                languageId: '1',
                keyword: 'test',
                archived: true
            });
        });
    });

    it('should get isFavoritePanelCollaped Params', () => {
        dotPageStore.isFavoritePanelCollaped$.subscribe((data) => {
            expect(data).toEqual(true);
        });
    });

    it('should get pages loading status', () => {
        dotPageStore.isPagesLoading$.subscribe((data) => {
            expect(data).toEqual(true);
        });
    });

    it('should get portlet loading status', () => {
        dotPageStore.isPortletLoading$.subscribe((data) => {
            expect(data).toEqual(false);
        });
    });

    // Updaters
    it('should update Favorite Pages', () => {
        dotPageStore.setFavoritePages({ items: favoritePagesInitialTestData });
        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.items).toEqual(favoritePagesInitialTestData);
        });
    });

    it('should update Pages', () => {
        dotPageStore.setPages({ items: favoritePagesInitialTestData });
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.items).toEqual(favoritePagesInitialTestData);
        });
    });

    it('should update Keyword', () => {
        dotPageStore.setKeyword('test');
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.keyword).toEqual('test');
        });
    });

    it('should update LanguageId', () => {
        dotPageStore.setLanguageId('1');
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.languageId).toEqual('1');
        });
    });

    it('should update Archived', () => {
        dotPageStore.setArchived('true');
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.archived).toEqual(true);
        });
    });

    it('should update Session Storage Filter Params', () => {
        // Mock sessionStorage.setItem
        (sessionStorage.setItem as jest.Mock).mockImplementation(() => {
            //
        });
        dotPageStore.setSessionStorageFilterParams();
        expect(sessionStorage.setItem).toHaveBeenCalledWith(
            SESSION_STORAGE_FAVORITES_KEY,
            '{"keyword":"test","languageId":"1","archived":true}'
        );
    });

    it('should update Local Storage Panel Collapsed Params', () => {
        jest.spyOn(dotLocalstorageService, 'setItem');
        dotPageStore.setLocalStorageFavoritePanelCollapsedParams(true);
        expect(dotLocalstorageService.setItem).toHaveBeenCalledWith(
            LOCAL_STORAGE_FAVORITES_PANEL_KEY,
            'true'
        );
    });

    it('should have favorites collapsed state setted when requesting favorite pages', () => {
        dotPageStore.getFavoritePages(5); // Here it sets the favorite state again

        // Should retrieve the value from local storage when setting the state
        expect(dotLocalstorageService.getItem).toHaveBeenCalledWith(
            LOCAL_STORAGE_FAVORITES_PANEL_KEY
        );

        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.collapsed).toEqual(true);
        });
    });

    it('should update Pages Status', () => {
        dotPageStore.setPagesStatus(ComponentStatus.LOADING);
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.status).toEqual(ComponentStatus.LOADING);
        });
    });

    it('should update Portlet Status', () => {
        dotPageStore.setPortletStatus(ComponentStatus.LOADING);
        dotPageStore.state$.subscribe((data) => {
            expect(data.portletStatus).toEqual(ComponentStatus.LOADING);
        });
    });

    it('should clear Menu Actions', () => {
        dotPageStore.clearMenuActions();
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.menuActions).toEqual([]);
            expect(data.pages.actionMenuDomId).toEqual(null);
        });
    });

    it('should set Menu Actions', () => {
        const tmpData = { actions: [{ label: 'test' }], actionMenuDomId: 'test1' };
        dotPageStore.setMenuActions(tmpData);
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.menuActions).toEqual(tmpData.actions);
            expect(data.pages.actionMenuDomId).toEqual(tmpData.actionMenuDomId);
        });
    });

    it('should update Add to Bundle CT Id', () => {
        dotPageStore.showAddToBundle('test1');
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.addToBundleCTId).toEqual('test1');
        });
    });

    // Effects
    it('should set all Favorite Pages value in store', () => {
        const expectedInputArray = [
            ...favoritePagesInitialTestData,
            ...favoritePagesInitialTestData
        ];
        jest.spyOn(dotFavoritePageService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: expectedInputArray as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 4
            })
        );
        dotPageStore.getFavoritePages(4);

        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.items).toEqual(expectedInputArray);
            expect(data.favoritePages.showLoadMoreButton).toEqual(true);
            expect(data.favoritePages.total).toEqual(expectedInputArray.length);
            expect(data.favoritePages.collapsed).toEqual(true);
        });
        expect(dotFavoritePageService.get).toHaveBeenCalledTimes(1);
    });

    it('should get all Page Types value in store and show dialog', () => {
        const expectedInputArray = [{ ...dotcmsContentTypeBasicMock, ...contentTypeDataMock[0] }];
        jest.spyOn(dotPageTypesService, 'getPages').mockReturnValue(
            of(expectedInputArray as unknown as DotCMSContentType[])
        );
        dotPageStore.getPageTypes();

        dotPageStore.state$.subscribe((data) => {
            expect(data.pageTypes).toEqual(expectedInputArray as unknown as DotCMSContentType[]);
        });
        expect(dotPageTypesService.getPages).toHaveBeenCalledTimes(1);
        expect(dialogService.open).toHaveBeenCalledWith(DotPagesCreatePageDialogComponent, {
            header: 'create.page',
            width: '58rem',
            data: {
                pageTypes: expectedInputArray
            }
        });
    });

    it('should set all Pages value in store', () => {
        const expectedInputArray = [
            {
                ...favoritePagesInitialTestData[0]
            },
            {
                ...favoritePagesInitialTestData[1]
            }
        ];
        jest.spyOn(dotESContentService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: favoritePagesInitialTestData as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 4
            })
        );
        dotPageStore.getPages({ offset: 0, sortField: 'title', sortOrder: 1 });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.items).toEqual(expectedInputArray);
        });
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);
        expect(dotESContentService.get).toHaveBeenCalledWith({
            itemsPerPage: 40,
            offset: '0',
            query: '+conhost:123-xyz-567-xxl +working:true  +(urlmap:* OR basetype:5) +languageId:1 +deleted:true +(title:test* OR path:*test* OR urlmap:*test*) ',
            sortField: 'title',
            sortOrder: ESOrderDirection.ASC
        });
    });

    it('should set Pages to empty when changed from a Site with data to an empty one', () => {
        const pagesData = [
            {
                ...favoritePagesInitialTestData[0]
            },
            {
                ...favoritePagesInitialTestData[1]
            }
        ];

        dotPageStore.setPages({ items: pagesData });

        jest.spyOn(dotESContentService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: []
                },
                queryTook: 1,
                resultsSize: 0
            })
        );
        dotPageStore.getPages({ offset: 0, sortField: 'title', sortOrder: 1 });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.items).toEqual([]);
        });
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);
    });

    it('should handle error when get Pages value fails', () => {
        const error500 = mockResponseView(500, '/test', null, { message: 'error' });
        jest.spyOn(dotESContentService, 'get').mockReturnValue(throwError(error500));
        dotPageStore.getPages({ offset: 0, sortField: 'title', sortOrder: 1 });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.status).toEqual(ComponentStatus.LOADED);
        });
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error500, true);
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
    });

    it('should remove page archived from pages collection and add undefined at the bottom', fakeAsync(() => {
        dotPageStore.setPages({ items: favoritePagesInitialTestData });

        const updated = {
            contentTook: 0,
            jsonObjectView: {
                contentlets: [] as unknown as DotCMSContentlet[]
            },
            queryTook: 1,
            resultsSize: 4
        };

        jest.spyOn(dotESContentService, 'get').mockReturnValue(of(updated));

        dotPageStore.updateSinglePageData({
            identifier: '123',
            isFavoritePage: false
        });

        tick(3000);

        // Testing page archived removed from pages collection and added undefined at the bottom
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.items).toEqual([favoritePagesInitialTestData[1], undefined]);
        });
    }));

    it('should get all Workflow actions and static actions from a contentlet', () => {
        const expectedInputArray = [{ ...dotcmsContentTypeBasicMock, ...contentTypeDataMock[0] }];
        jest.spyOn(dotWorkflowsActionsService, 'getByInode').mockReturnValue(
            of(mockWorkflowsActions)
        );
        jest.spyOn(dotFavoritePageService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: expectedInputArray as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 4
            })
        );
        dotPageStore.showActionsMenu({
            item: favoritePagesInitialTestData[0],
            actionMenuDomId: 'test1'
        });

        dotPageStore.state$.subscribe((data) => {
            const menuActions = data.pages.menuActions;

            expect(menuActions.length).toEqual(9);

            expect(menuActions[0].label).toEqual('favoritePage.contextMenu.action.edit');
            expect(menuActions[1].label).toEqual('favoritePage.dialog.delete.button');
            expect(menuActions[2].label).toEqual(undefined);
            expect(menuActions[3].label).toEqual('Edit');
            expect(menuActions[4].label).toEqual(mockWorkflowsActions[0].name);
            expect(menuActions[5].label).toEqual(mockWorkflowsActions[1].name);
            expect(menuActions[6].label).toEqual(mockWorkflowsActions[2].name);
            expect(menuActions[7].label).toEqual('contenttypes.content.push_publish');
            expect(menuActions[8].label).toEqual('contenttypes.content.add_to_bundle');

            expect(data.pages.actionMenuDomId).toEqual('test1');
        });

        expect(dotFavoritePageService.get).toHaveBeenCalledWith({
            identifier: undefined,
            limit: 1,
            userId: 'testId',
            url: '/index1?&language_id=1&device_inode=',
            sortOrder: 'ASC, identifier'
        });
        expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledWith(
            favoritePagesInitialTestData[0].inode,
            DotRenderMode.LISTING
        );
    });

    it('should trigger push publish dialog with the correct item identifier', (done) => {
        const expectedInputArray = [{ ...dotcmsContentTypeBasicMock, ...contentTypeDataMock[0] }];

        const item = favoritePagesInitialTestData[0];

        jest.spyOn(dotWorkflowsActionsService, 'getByInode').mockReturnValue(
            of(mockWorkflowsActions)
        );
        jest.spyOn(dotFavoritePageService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: expectedInputArray as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 4
            })
        );

        jest.spyOn(dotPushPublishDialogService, 'open');

        dotPageStore.showActionsMenu({
            item,
            actionMenuDomId: 'test1'
        });

        dotPageStore.state$.subscribe((data) => {
            const menuActions = data.pages.menuActions;

            expect(menuActions[7].label).toEqual('contenttypes.content.push_publish');

            menuActions[7].command({ originalEvent: createFakeEvent('click') });

            expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                assetIdentifier: item.identifier,
                title: 'contenttypes.content.push_publish'
            });

            done();
        });
    });

    it('should get all Workflow actions and static actions from a favorite page', () => {
        jest.spyOn(dotPageWorkflowsActionsService, 'getByUrl').mockReturnValue(
            of({ actions: mockWorkflowsActions, page: dotcmsContentletMock })
        );
        dotPageStore.showActionsMenu({
            item: {
                ...favoritePagesInitialTestData[0],
                contentType: 'dotFavoritePage'
            },
            actionMenuDomId: 'test1'
        });

        expect(dotPageWorkflowsActionsService.getByUrl).toHaveBeenCalledWith({
            host_id: 'A',
            language_id: '1',
            url: '/index1'
        });
    });

    it('should not have Add/Edit Bookmark actions in context menu when contentlet is archived', () => {
        jest.spyOn(dotPageWorkflowsActionsService, 'getByUrl').mockReturnValue(
            of({ actions: mockWorkflowsActions, page: dotcmsContentletMock })
        );

        dotPageStore.showActionsMenu({
            item: {
                ...favoritePagesInitialTestData[1],
                url: '/index2?host_id=A&language_id=1&device_inode=123',
                contentType: 'dotFavoritePage',
                archived: true
            },
            actionMenuDomId: 'test1'
        });

        expect(dotPageWorkflowsActionsService.getByUrl).toHaveBeenCalledWith({
            host_id: 'A',
            language_id: '1',
            url: '/index2'
        });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.menuActions.length).toEqual(8);
            expect(data.pages.menuActions[0].label).toEqual('favoritePage.contextMenu.action.edit');
            expect(data.pages.menuActions[1].label).toEqual('favoritePage.dialog.delete.button');
        });
    });

    it('should get all menu actions from a favorite page when page is archived', () => {
        jest.spyOn(dotPageWorkflowsActionsService, 'getByUrl').mockReturnValue(
            of({ actions: mockWorkflowsActions, page: dotcmsContentletMock })
        );

        dotPageStore.showActionsMenu({
            item: {
                ...favoritePagesInitialTestData[0],
                contentType: 'dotFavoritePage',
                archived: true
            },
            actionMenuDomId: 'test1'
        });

        expect(dotPageWorkflowsActionsService.getByUrl).toHaveBeenCalledWith({
            host_id: 'A',
            language_id: '1',
            url: '/index1'
        });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.menuActions[0].label).toEqual('favoritePage.contextMenu.action.edit');
            expect(data.pages.menuActions[1].label).toEqual('favoritePage.dialog.delete.button');
            expect(data.pages.menuActions[2]).toEqual({ separator: true });
            expect(data.pages.menuActions[3].label).toEqual('Assign Workflow');
            expect(data.pages.menuActions[4].label).toEqual('Save');
            expect(data.pages.menuActions[5].label).toEqual('Save / Publish');
            expect(data.pages.menuActions[6].label).toEqual('contenttypes.content.push_publish');
            expect(data.pages.menuActions[7].label).toEqual('contenttypes.content.add_to_bundle');
        });
    });

    it('should delete a Favorite Pages value in store', () => {
        const expectedInputArray = [
            ...favoritePagesInitialTestData,
            ...favoritePagesInitialTestData
        ];
        const testInode = '12345';

        jest.spyOn(dotWorkflowActionsFireService, 'deleteContentlet').mockReturnValue(
            of(testInode)
        );
        jest.spyOn(dotESContentService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: expectedInputArray as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 4
            })
        );

        dotPageStore.deleteFavoritePage(testInode);

        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.items).toEqual(expectedInputArray);
            expect(data.favoritePages.showLoadMoreButton).toEqual(true);
            expect(data.favoritePages.total).toEqual(expectedInputArray.length);
        });
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);
        expect(dotWorkflowActionsFireService.deleteContentlet).toHaveBeenCalledWith({
            inode: testInode
        });
    });

    it('should call deleteFavoritePage as much times as we need', () => {
        const testInode = '12345';

        jest.spyOn(dotWorkflowActionsFireService, 'deleteContentlet').mockReturnValue(
            of(testInode)
        );

        dotPageStore.deleteFavoritePage(testInode);
        dotPageStore.deleteFavoritePage(testInode);
        dotPageStore.deleteFavoritePage(testInode);
        dotPageStore.deleteFavoritePage(testInode);
        dotPageStore.deleteFavoritePage(testInode);

        expect(dotWorkflowActionsFireService.deleteContentlet).toHaveBeenCalledTimes(5);
    });

    it('should handle error when a Workflow Action Request fails', (done) => {
        const actions = [mockPublishAction];
        const page = dotcmsContentletMock;
        const error: HttpErrorResponse = new HttpErrorResponse({
            error: {
                message:
                    'The Workflow Action is not available in the Workflow Step the content is currently in.'
            }
        });
        const item = {
            ...favoritePagesInitialTestData[0],
            contentType: 'dotFavoritePage',
            archived: true
        };

        jest.spyOn(dotPageWorkflowsActionsService, 'getByUrl').mockReturnValue(
            of({ actions, page })
        );
        jest.spyOn(dotWorkflowActionsFireService, 'fireTo').mockReturnValue(throwError(error));

        dotPageStore.showActionsMenu({ item, actionMenuDomId: 'test1' });

        dotPageStore.state$.subscribe(({ pages }) => {
            const menuAction = pages.menuActions;
            const publishAction = menuAction.find(
                (action) => action.label === mockPublishAction.name
            );
            publishAction.command({ originalEvent: createFakeEvent('click') });
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error, true);
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            done();
        });
    });
});
