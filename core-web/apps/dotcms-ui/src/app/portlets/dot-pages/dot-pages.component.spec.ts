import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpHandler, HttpResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import { of } from 'rxjs/internal/observable/of';

import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotEventsService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLanguagesService,
    DotMessageDisplayService,
    DotPageRenderService,
    DotRouterService,
    DotSessionStorageService,
    DotUiColorsService,
    DotESContentService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotWorkflowsActionsService,
    DotWorkflowEventHandlerService,
    PushPublishService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    CoreWebServiceMock,
    DotcmsEventsService,
    DotPushPublishDialogService,
    HttpCode,
    LoggerService,
    LoginService,
    mockSites,
    SiteService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { ComponentStatus, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import {
    dotcmsContentletMock,
    dotcmsContentTypeBasicMock,
    DotcmsEventsServiceMock,
    LoginServiceMock,
    mockResponseView,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotPageStore } from './dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams, DotPagesComponent } from './dot-pages.component';

import { IframeOverlayService } from '../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotContentletEditorService } from '../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

@Component({
    selector: 'dot-pages-favorite-panel',
    template: ''
})
class MockDotPagesFavoritePanelComponent {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();
}

@Component({
    selector: 'dot-pages-listing-panel',
    template: ''
})
class MockDotPagesListingPanelComponent {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();
}

@Component({
    selector: 'dot-add-to-bundle',
    template: ''
})
class MockDotAddToBundleComponent {
    @Input() assetIdentifier: string;
    @Output() cancel = new EventEmitter<boolean>();
}

export const favoritePagesInitialTestData = [
    {
        ...dotcmsContentletMock,
        live: true,
        baseType: 'CONTENT',
        identifier: '123',
        modDate: '2020-09-02 16:45:15.569',
        title: 'preview1',
        screenshot: 'test1',
        url: '/index1?host_id=A&language_id=1&device_inode=123',
        owner: 'admin'
    },
    {
        ...dotcmsContentletMock,
        title: 'preview2',
        modDate: '2020-09-02 16:45:15.569',
        identifier: '456',
        screenshot: 'test2',
        url: '/index2',
        owner: 'admin2'
    }
];

const storeMock = {
    get actionMenuDomId$() {
        return of('');
    },
    get languageOptions$() {
        return of([]);
    },
    get languageLabels$() {
        return of({});
    },
    get pageTypes$() {
        return of([{ ...dotcmsContentTypeBasicMock }]);
    },
    clearMenuActions: jest.fn(),
    getFavoritePages: jest.fn(),
    getPages: jest.fn(),
    getPageTypes: jest.fn(),
    showActionsMenu: jest.fn(),
    setInitialStateData: jest.fn(),
    limitFavoritePages: jest.fn(),
    setPortletStatus: jest.fn(),
    updateSinglePageData: jest.fn(),
    setLocalStorageFavoritePanelCollapsedParams: jest.fn(),
    setFavoritePages: jest.fn(),
    vm$: of({
        favoritePages: {
            items: [],
            showLoadMoreButton: false,
            total: 0
        },
        isEnterprise: true,
        environments: true,
        languages: [],
        loggedUser: {
            id: 'admin',
            canRead: { contentlets: true, htmlPages: true },
            canWrite: { contentlets: true, htmlPages: true }
        },
        pages: {
            actionMenuDomId: '',
            items: [],
            addToBundleCTId: 'test1'
        },
        pageTypes: [],
        portletStatus: ComponentStatus.LOADED
    })
};

class DotContentletEditorServiceMock {
    get createUrl$(): Observable<unknown> {
        return of(undefined);
    }
}

describe('DotPagesComponent', () => {
    let spectator: Spectator<DotPagesComponent>;
    let store: DotPageStore;
    let dotRouterService: DotRouterService;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotPageRenderService: DotPageRenderService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let siteServiceMock: SiteServiceMock;

    const createComponent = createComponentFactory({
        component: DotPagesComponent,
        imports: [
            MenuModule,
            MockDotPagesFavoritePanelComponent,
            MockDotPagesListingPanelComponent,
            MockDotAddToBundleComponent
        ],
        providers: [
            DotSessionStorageService,
            DotCurrentUserService,
            DotESContentService,
            DotPageTypesService,
            DotEventsService,
            DotWorkflowsActionsService,
            PushPublishService,
            DotWorkflowEventHandlerService,
            DialogService,
            DotLanguagesService,
            DotPushPublishDialogService,
            DotPageWorkflowsActionsService,
            HttpClient,
            HttpHandler,
            DotIframeService,
            DotFormatDateService,
            DotAlertConfirmService,
            ConfirmationService,
            DotUiColorsService,
            IframeOverlayService,
            LoggerService,
            StringUtils,
            ApiRoot,
            UserModel,
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            {
                provide: DotContentletEditorService,
                useValue: new DotContentletEditorServiceMock()
            },
            { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
            {
                provide: ActivatedRoute,
                useValue: {
                    get data() {
                        return of({ url: undefined });
                    }
                }
            },
            mockProvider(SiteService)
        ],
        detectChanges: false
    });

    beforeEach(() => {
        // Mock the DOM scroll method
        Element.prototype.scroll = jest.fn();

        siteServiceMock = new SiteServiceMock();

        // Create spies for the services before creating the component
        const dotPageRenderServiceSpy = {
            checkPermission: jest.fn().mockReturnValue(of(true))
        };
        const dotHttpErrorManagerServiceSpy = {
            handle: jest.fn().mockReturnValue(of(null))
        };
        const dotRouterServiceSpy = {
            goToEditPage: jest.fn()
        };
        const dotMessageDisplayServiceSpy = {
            push: jest.fn()
        };

        spectator = createComponent({
            providers: [
                { provide: DotPageStore, useValue: storeMock },
                { provide: SiteService, useValue: siteServiceMock },
                { provide: DotPageRenderService, useValue: dotPageRenderServiceSpy },
                { provide: DotHttpErrorManagerService, useValue: dotHttpErrorManagerServiceSpy },
                { provide: DotRouterService, useValue: dotRouterServiceSpy },
                { provide: DotMessageDisplayService, useValue: dotMessageDisplayServiceSpy }
            ]
        });

        store = spectator.inject(DotPageStore);
        dotRouterService = spectator.inject(DotRouterService);
        dotMessageDisplayService = spectator.inject(DotMessageDisplayService);
        dotPageRenderService = spectator.inject(DotPageRenderService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);

        spectator.detectChanges();
        jest.spyOn(spectator.component.menu, 'hide');
        jest.spyOn(spectator.component, 'scrollToTop');
    });

    it('should init store', () => {
        expect(store.setInitialStateData).toHaveBeenCalledWith(500);
        expect(store.setInitialStateData).toHaveBeenCalledTimes(1);
    });

    it('should have favorite page panel, menu, pages panel and DotAddToBundle components', () => {
        expect(spectator.query('dot-pages-favorite-panel')).toBeTruthy();
        expect(spectator.query('p-menu')).toBeTruthy();
        expect(spectator.query('dot-pages-listing-panel')).toBeTruthy();
        expect(spectator.query('dot-add-to-bundle')).toBeTruthy();
    });

    it('should call goToUrl method from DotPagesFavoritePanel', () => {
        spectator.triggerEventHandler('dot-pages-favorite-panel', 'goToUrl', '/page/1?lang=1');

        expect(dotPageRenderService.checkPermission).toHaveBeenCalledWith({
            lang: '1',
            url: '/page/1'
        });
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            lang: '1',
            url: '/page/1'
        });
    });

    it('should call goToUrl method from DotPagesFavoritePanel and throw User permission error', () => {
        dotPageRenderService.checkPermission = jest.fn().mockReturnValue(of(false));

        spectator.triggerEventHandler('dot-pages-favorite-panel', 'goToUrl', '/page/1?lang=1');

        expect(store.setPortletStatus).toHaveBeenCalledWith(ComponentStatus.LOADING);
        // setPortletStatus is called multiple times during the flow
        expect(store.setPortletStatus).toHaveBeenCalledTimes(3);
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
            new HttpErrorResponse(
                new HttpResponse({
                    body: null,
                    status: HttpCode.FORBIDDEN,
                    headers: null,
                    url: ''
                })
            )
        );
    });

    it('should throw error dialog when call GoTo and url does not match with existing page', () => {
        const error404 = mockResponseView(404);
        dotPageRenderService.checkPermission = jest.fn().mockReturnValue(throwError(error404));

        spectator.triggerEventHandler('dot-pages-favorite-panel', 'goToUrl', '/page/1?lang=1');

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error404);
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        expect(store.setPortletStatus).toHaveBeenCalledWith(ComponentStatus.LOADED);
        // setPortletStatus is called multiple times during the flow
        expect(store.setPortletStatus).toHaveBeenCalledTimes(5);
    });

    it('should call showActionsMenu method from DotPagesFavoritePanel', () => {
        const eventMock = new MouseEvent('click');
        Object.defineProperty(eventMock, 'currentTarget', {
            value: { id: 'test' },
            enumerable: true
        });

        const actionMenuParam = {
            event: eventMock,
            actionMenuDomId: 'test1',
            item: dotcmsContentletMock
        };

        spectator.triggerEventHandler(
            'dot-pages-favorite-panel',
            'showActionsMenu',
            actionMenuParam
        );

        expect(spectator.component.menu.hide).toHaveBeenCalledTimes(1);
        expect(store.showActionsMenu).toHaveBeenCalledWith({
            item: dotcmsContentletMock,
            actionMenuDomId: 'test1'
        });
    });

    it('should call goToUrl method from DotPagesListingPanel', () => {
        spectator.triggerEventHandler('dot-pages-listing-panel', 'goToUrl', '/page/1?lang=1');

        expect(store.setPortletStatus).toHaveBeenCalledWith(ComponentStatus.LOADING);
        // setPortletStatus is called multiple times during the flow
        expect(store.setPortletStatus).toHaveBeenCalledTimes(6);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            lang: '1',
            url: '/page/1'
        });
    });

    it('should call showActionsMenu method from DotPagesListingPanel', () => {
        const eventMock = new MouseEvent('click');
        Object.defineProperty(eventMock, 'currentTarget', {
            value: { id: 'test' },
            enumerable: true
        });

        const actionMenuParam = {
            event: eventMock,
            actionMenuDomId: 'test1',
            item: dotcmsContentletMock
        };

        spectator.triggerEventHandler(
            'dot-pages-listing-panel',
            'showActionsMenu',
            actionMenuParam
        );

        expect(spectator.component.menu.hide).toHaveBeenCalledTimes(1);
        expect(store.showActionsMenu).toHaveBeenCalledWith({
            item: dotcmsContentletMock,
            actionMenuDomId: 'test1'
        });
    });

    it('should call scrollToTop method from DotPagesListingPanel', () => {
        spectator.triggerEventHandler('[data-testId="pages-listing-panel"]', 'pageChange', null);

        expect(spectator.component.scrollToTop).toHaveBeenCalled();
    });

    it('should call closedActionsMenu method from p-menu', () => {
        spectator.component.closedActionsMenu = jest.fn();
        spectator.triggerEventHandler('p-menu', 'onHide', {});

        expect(spectator.component.closedActionsMenu).toHaveBeenCalledTimes(1);
    });

    it('should call push method in dotMessageDisplayService once a save-page is received for a non favorite page', () => {
        const dotEventsService = spectator.inject(DotEventsService);

        dotEventsService.notify('save-page', {
            payload: { identifier: '123' },
            value: 'test3'
        });

        expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
            life: 3000,
            message: 'test3',
            severity: DotMessageSeverity.SUCCESS,
            type: DotMessageType.SIMPLE_MESSAGE
        });
        expect(store.updateSinglePageData).toHaveBeenCalledWith({
            identifier: '123',
            isFavoritePage: false
        });
    });

    it('should update a single page once a save-page is received for a favorite page', () => {
        const dotEventsService = spectator.inject(DotEventsService);

        dotEventsService.notify('save-page', {
            payload: { contentType: 'dotFavoritePage', identifier: '123' },
            value: 'test3'
        });

        expect(store.updateSinglePageData).toHaveBeenCalledWith({
            identifier: '123',
            isFavoritePage: true
        });
    });

    it('should trigger getPages when deactivating the router-outlet', () => {
        spectator.triggerEventHandler('router-outlet', 'activate', null);
        spectator.detectChanges();
        spectator.triggerEventHandler('router-outlet', 'deactivate', null);
        spectator.detectChanges();

        expect(store.getPages).toHaveBeenCalled();
    });

    it('should reload portlet only when the site change', () => {
        const initialCallCount = (store.getPages as jest.Mock).mock.calls.length;
        siteServiceMock.setFakeCurrentSite(mockSites[1]); // switching the site
        expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
        // Verify getPages was called at least once more after site change
        expect((store.getPages as jest.Mock).mock.calls.length).toBeGreaterThan(initialCallCount);
        expect(spectator.component.scrollToTop).toHaveBeenCalled();
    });
});
