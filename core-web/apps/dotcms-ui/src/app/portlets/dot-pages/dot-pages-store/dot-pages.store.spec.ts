import { Observable, of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { PushPublishServiceMock } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.component.spec';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotWizardService } from '@dotcms/app/api/services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { MockDotHttpErrorManagerService } from '@dotcms/app/test/dot-http-error-manager.service.mock';
import {
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotLanguagesService,
    DotLicenseService,
    DotPageTypesService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    ESOrderDirection
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
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
    DotcmsConfigServiceMock,
    dotcmsContentTypeBasicMock,
    DotcmsEventsServiceMock,
    DotLanguagesServiceMock,
    LoginServiceMock,
    mockDotLanguage,
    MockDotRouterService,
    mockResponseView,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { DotPageStore } from './dot-pages.store';

import { contentTypeDataMock } from '../../dot-edit-page/components/dot-palette/dot-palette-content-type/dot-palette-content-type.component.spec';
import { DotLicenseServiceMock } from '../../dot-edit-page/content/services/html/dot-edit-content-toolbar-html.service.spec';
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
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

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
                DotWorkflowEventHandlerService,
                LoggerService,
                StringUtils,
                { provide: DialogService, useClass: DialogServiceMock },
                { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
                { provide: DotcmsConfigService, useClass: DotcmsConfigServiceMock },
                { provide: DotLanguagesService, useClass: DotLanguagesServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotHttpErrorManagerService, useClass: MockDotHttpErrorManagerService },
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

        spyOn(dialogService, 'open').and.callThrough();
        spyOn(dotHttpErrorManagerService, 'handle');

        dotPageStore.setInitialStateData(5);
    });

    it('should load Favorite Pages initial data', () => {
        dotPageStore.state$.subscribe((data) => {
            expect(data.environments).toEqual(true);
            expect(data.favoritePages.items).toEqual(favoritePagesInitialTestData);
            expect(data.favoritePages.showLoadMoreButton).toEqual(false);
            expect(data.favoritePages.total).toEqual(favoritePagesInitialTestData.length);
            expect(data.isEnterprise).toEqual(true);
            expect(data.languages).toEqual([mockDotLanguage]);
            expect(data.loggedUser.id).toEqual(CurrentUserDataMock.userId);
            expect(data.loggedUser.canRead).toEqual({ contentlets: true, htmlPages: true });
            expect(data.loggedUser.canWrite).toEqual({ contentlets: true, htmlPages: true });
            expect(data.pages.items).toEqual([]);
            expect(data.pages.keyword).toEqual('');
            expect(data.pages.status).toEqual(ComponentStatus.INIT);
        });
    });

    it('should load null Favorite Pages data when error on initial data fetch', () => {
        const error500 = mockResponseView(500, '/test', null, { message: 'error' });
        spyOn(dotESContentService, 'get').and.returnValue(throwError(error500));
        dotPageStore.setInitialStateData(5);

        dotPageStore.state$.subscribe((data) => {
            expect(data.environments).toEqual(false);
            expect(data.favoritePages.items).toEqual([]);
            expect(data.favoritePages.showLoadMoreButton).toEqual(false);
            expect(data.favoritePages.total).toEqual(0);
            expect(data.isEnterprise).toEqual(false);
            expect(data.languages).toEqual(null);
            expect(data.loggedUser.id).toEqual(null);
            expect(data.loggedUser.canRead).toEqual({ contentlets: null, htmlPages: null });
            expect(data.loggedUser.canWrite).toEqual({ contentlets: null, htmlPages: null });
            expect(data.pages.items).toEqual([]);
            expect(data.pages.keyword).toEqual('');
            expect(data.pages.status).toEqual(ComponentStatus.INIT);
        });
    });

    it('should limit Favorite Pages', () => {
        spyOn(dotPageStore, 'setFavoritePages').and.callThrough();
        dotPageStore.limitFavoritePages(5);
        expect(dotPageStore.setFavoritePages).toHaveBeenCalledWith(
            favoritePagesInitialTestData.slice(0, 5)
        );
    });

    // Selectors
    it('should get language options for dropdown', () => {
        dotPageStore.languageOptions$.subscribe((data) => {
            expect(data).toEqual([
                { label: 'All', value: null },
                { label: 'English (US)', value: 1 }
            ]);
        });
    });

    it('should get language Labels for row field', () => {
        dotPageStore.languageLabels$.subscribe((data) => {
            expect(data).toEqual({ '1': 'en-US' });
        });
    });

    it('should get pages status', () => {
        dotPageStore.getStatus$.subscribe((data) => {
            expect(data).toEqual(ComponentStatus.INIT);
        });
    });

    it('should get pages loading status', () => {
        dotPageStore.isPagesLoading$.subscribe((data) => {
            expect(data).toEqual(true);
        });
    });

    // Updaters
    it('should update Favorite Pages', () => {
        dotPageStore.setFavoritePages(favoritePagesInitialTestData);
        dotPageStore.state$.subscribe((data) => {
            expect(data.favoritePages.items).toEqual(favoritePagesInitialTestData);
        });
    });

    it('should update Pages', () => {
        dotPageStore.setPages(favoritePagesInitialTestData);
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

    it('should update Pages Status', () => {
        dotPageStore.setPagesStatus(ComponentStatus.LOADING);
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.status).toEqual(ComponentStatus.LOADING);
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
        spyOn(dotESContentService, 'get').and.returnValue(
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
        });
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);
    });

    it('should get all Page Types value in store and show dialog', () => {
        const expectedInputArray = [{ ...dotcmsContentTypeBasicMock, ...contentTypeDataMock[0] }];
        spyOn(dotPageTypesService, 'getPages').and.returnValue(
            of(expectedInputArray as unknown as DotCMSContentType[])
        );
        dotPageStore.getPageTypes();

        dotPageStore.state$.subscribe((data) => {
            expect(data.pageTypes).toEqual(expectedInputArray);
        });
        expect(dotPageTypesService.getPages).toHaveBeenCalledTimes(1);
        expect(dialogService.open).toHaveBeenCalledWith(DotPagesCreatePageDialogComponent, {
            header: 'create.page',
            width: '58rem',
            data: expectedInputArray
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
        spyOn(dotESContentService, 'get').and.returnValue(
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
            query: '+conhost:123-xyz-567-xxl +working:true  +(urlmap:* OR basetype:5)    ',
            sortField: 'title',
            sortOrder: ESOrderDirection.ASC
        });
    });

    it('should handle error when get Pages value fails', () => {
        const error500 = mockResponseView(500, '/test', null, { message: 'error' });
        spyOn(dotESContentService, 'get').and.returnValue(throwError(error500));
        dotPageStore.getPages({ offset: 0, sortField: 'title', sortOrder: 1 });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.status).toEqual(ComponentStatus.LOADED);
        });
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error500);
    });

    it('should keep fetching Pages data until new value comes from the DB in store', fakeAsync(() => {
        dotPageStore.setPages(favoritePagesInitialTestData);
        const old = {
            contentTook: 0,
            jsonObjectView: {
                contentlets: favoritePagesInitialTestData as unknown as DotCMSContentlet[]
            },
            queryTook: 1,
            resultsSize: 2
        };

        const updated = {
            contentTook: 0,
            jsonObjectView: {
                contentlets: [
                    { ...favoritePagesInitialTestData[0], modDate: '2020-09-02 16:50:15.569' },
                    { ...favoritePagesInitialTestData[1] }
                ] as unknown as DotCMSContentlet[]
            },
            queryTook: 1,
            resultsSize: 4
        };

        const mockFunction = (times) => {
            let count = 1;

            return Observable.create((observer) => {
                if (count++ > times) {
                    observer.next(updated);
                } else {
                    observer.next(old);
                }
            });
        };

        spyOn(dotESContentService, 'get').and.returnValue(mockFunction(3));
        spyOn(dotPageStore, 'setPagesStatus').and.callThrough();

        dotPageStore.updateSinglePageData({ identifier: '123', isFavoritePage: false });

        tick(3000);

        // dotESContentService.get only is called 1 time, but "retryWhen" operator makes several request to the SpyOn
        expect(dotESContentService.get).toHaveBeenCalledTimes(1);

        // Testing to setPagesStatus to LOADING on the first fetch
        expect((dotPageStore.setPagesStatus as jasmine.Spy).calls.argsFor(0).toString()).toBe(
            ComponentStatus.LOADING
        );

        // Testing to pages.status to be LOADED on the last fetch (there can only be 2 calls during the whole process)
        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.status).toBe(ComponentStatus.LOADED);
        });

        // Since dotESContentService.get can only be called 1 time (and once called the data will be changed on "mockFunction"),
        // we test that the last fetch contains the updated data
        (dotESContentService.get as jasmine.Spy).calls
            .mostRecent()
            .returnValue.subscribe((data) => {
                expect(data).toEqual(updated);
            });
    }));

    it('should get all Workflow actions and static actions from a contentlet', () => {
        spyOn(dotWorkflowsActionsService, 'getByInode').and.returnValue(of(mockWorkflowsActions));
        dotPageStore.showActionsMenu({
            item: favoritePagesInitialTestData[0],
            actionMenuDomId: 'test1'
        });

        dotPageStore.state$.subscribe((data) => {
            expect(data.pages.menuActions.length).toEqual(6);
            expect(data.pages.menuActions[0].label).toEqual('Edit');
            expect(data.pages.menuActions[1].label).toEqual(mockWorkflowsActions[0].name);
            expect(data.pages.menuActions[2].label).toEqual(mockWorkflowsActions[1].name);
            expect(data.pages.menuActions[3].label).toEqual(mockWorkflowsActions[2].name);
            expect(data.pages.menuActions[4].label).toEqual('contenttypes.content.push_publish');
            expect(data.pages.menuActions[5].label).toEqual('contenttypes.content.add_to_bundle');
            expect(data.pages.actionMenuDomId).toEqual('test1');
        });

        expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledWith(
            favoritePagesInitialTestData[0].inode,
            DotRenderMode.LISTING
        );
    });
});
