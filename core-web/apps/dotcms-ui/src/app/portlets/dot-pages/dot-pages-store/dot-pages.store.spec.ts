import { formatDistanceStrict } from 'date-fns';
import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';

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
    DotWorkflowsActionsService
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
import { DotCMSContentlet, DotCMSContentType, ESContent } from '@dotcms/dotcms-models';
import {
    DotcmsConfigServiceMock,
    dotcmsContentTypeBasicMock,
    DotcmsEventsServiceMock,
    DotLanguagesServiceMock,
    LoginServiceMock,
    mockDotLanguage,
    MockDotRouterService,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { DotPageStore } from './dot-pages.store';

import { contentTypeDataMock } from '../../dot-edit-page/components/dot-palette/dot-palette-content-type/dot-palette-content-type.component.spec';
import { DotLicenseServiceMock } from '../../dot-edit-page/content/services/html/dot-edit-content-toolbar-html.service.spec';
import {
    CurrentUserDataMock,
    DotCurrentUserServiceMock
} from '../../dot-starter/dot-starter-resolver.service.spec';
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

describe('DotPageStore', () => {
    let dotPageStore: DotPageStore;
    let dotESContentService: DotESContentService;
    let dotPageTypesService: DotPageTypesService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;

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
        dotESContentService = TestBed.inject(DotESContentService);
        dotPageTypesService = TestBed.inject(DotPageTypesService);
        dotWorkflowsActionsService = TestBed.inject(DotWorkflowsActionsService);

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

    it('should get all Page Types value in store', () => {
        const expectedInputArray = [{ ...dotcmsContentTypeBasicMock, ...contentTypeDataMock[0] }];
        spyOn(dotPageTypesService, 'getPages').and.returnValue(
            of(expectedInputArray as unknown as DotCMSContentType[])
        );
        dotPageStore.getPageTypes();

        dotPageStore.state$.subscribe((data) => {
            expect(data.pageTypes).toEqual(expectedInputArray);
        });
        expect(dotPageTypesService.getPages).toHaveBeenCalledTimes(1);
    });

    it('should set all Pages value in store', () => {
        const relativeDate = (date: string) => {
            return formatDistanceStrict(
                new Date(parseInt(new Date(date).getTime().toString(), 10)),
                new Date(),
                {
                    addSuffix: true
                }
            );
        };

        const expectedInputArray = [
            {
                ...favoritePagesInitialTestData[0],
                modDate: relativeDate(favoritePagesInitialTestData[0].modDate)
            },
            {
                ...favoritePagesInitialTestData[1],
                modDate: relativeDate(favoritePagesInitialTestData[1].modDate)
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
    });

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
            expect(data.pages.menuActions[4].label).toEqual('contenttypes.content.add_to_bundle');
            expect(data.pages.menuActions[5].label).toEqual('contenttypes.content.push_publish');
            expect(data.pages.actionMenuDomId).toEqual('test1');
        });

        expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledWith(
            favoritePagesInitialTestData[0].inode,
            DotRenderMode.LISTING
        );
    });
});
