import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpHandler, HttpResponse } from '@angular/common/http';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { MenuModule } from 'primeng/menu';

import { of } from 'rxjs/internal/observable/of';

import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotCreateContentletComponent } from '@components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.component';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { MockDotHttpErrorManagerService } from '@dotcms/app/test/dot-http-error-manager.service.mock';
import {
    DotAlertConfirmService,
    DotEventsService,
    DotPageRenderService
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsEventsService,
    HttpCode,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    dotcmsContentletMock,
    dotcmsContentTypeBasicMock,
    DotcmsEventsServiceMock,
    LoginServiceMock,
    MockDotRouterService,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotPageStore } from './dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams, DotPagesComponent } from './dot-pages.component';

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
    clearMenuActions: jasmine.createSpy(),
    getFavoritePages: jasmine.createSpy(),
    getPages: jasmine.createSpy(),
    getPageTypes: jasmine.createSpy(),
    showActionsMenu: jasmine.createSpy(),
    setInitialStateData: jasmine.createSpy(),
    limitFavoritePages: jasmine.createSpy(),
    setPortletStatus: jasmine.createSpy(),
    updateSinglePageData: jasmine.createSpy(),
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
    let fixture: ComponentFixture<DotPagesComponent>;
    let component: DotPagesComponent;
    let de: DebugElement;
    let store: DotPageStore;
    let dotRouterService: DotRouterService;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotPageRenderService: DotPageRenderService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    const dotContentletEditorServiceMock: DotContentletEditorServiceMock =
        new DotContentletEditorServiceMock();

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                MockDotPagesFavoritePanelComponent,
                MockDotPagesListingPanelComponent,
                MockDotAddToBundleComponent,
                DotPagesComponent
            ],
            imports: [MenuModule],
            providers: [
                DotEventsService,
                HttpClient,
                HttpHandler,
                DotPageRenderService,
                DotIframeService,
                DotFormatDateService,
                DotAlertConfirmService,
                ConfirmationService,
                DotUiColorsService,
                IframeOverlayService,
                LoggerService,
                StringUtils,
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotContentletEditorService,
                    useValue: dotContentletEditorServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotcmsEventsService,
                    useClass: DotcmsEventsServiceMock
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        get data() {
                            return of({ url: undefined });
                        }
                    }
                }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        TestBed.overrideProvider(DotPageStore, {
            useValue: storeMock
        });
        store = TestBed.inject(DotPageStore);
        dotRouterService = TestBed.inject(DotRouterService);
        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
        dotPageRenderService = TestBed.inject(DotPageRenderService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        fixture = TestBed.createComponent(DotPagesComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;

        fixture.detectChanges();
        spyOn(component.menu, 'hide');
        spyOn(dotMessageDisplayService, 'push');
        spyOn(dotPageRenderService, 'checkPermission').and.returnValue(of(true));
        spyOn(dotHttpErrorManagerService, 'handle');
    });

    it('should init store', () => {
        expect(store.setInitialStateData).toHaveBeenCalledWith(500);
    });

    it('should have favorite page panel, menu, pages panel and DotAddToBundle components', () => {
        expect(de.query(By.css('dot-pages-favorite-panel'))).toBeTruthy();
        expect(de.query(By.css('p-menu'))).toBeTruthy();
        expect(de.query(By.css('dot-pages-listing-panel'))).toBeTruthy();
        expect(de.query(By.css('dot-add-to-bundle'))).toBeTruthy();
    });

    it('should call goToUrl method from DotPagesFavoritePanel', () => {
        const elem = de.query(By.css('dot-pages-favorite-panel'));
        elem.triggerEventHandler('goToUrl', '/page/1?lang=1');

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
        dotPageRenderService.checkPermission = jasmine.createSpy().and.returnValue(of(false));

        const elem = de.query(By.css('dot-pages-favorite-panel'));
        elem.triggerEventHandler('goToUrl', '/page/1?lang=1');

        expect(store.setPortletStatus).toHaveBeenCalledWith(ComponentStatus.LOADING);
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
        dotPageRenderService.checkPermission = jasmine
            .createSpy()
            .and.returnValue(throwError(error404));

        const elem = de.query(By.css('dot-pages-favorite-panel'));
        elem.triggerEventHandler('goToUrl', '/page/1?lang=1');

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error404);
        expect(store.setPortletStatus).toHaveBeenCalledWith(ComponentStatus.LOADED);
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

        const elem = de.query(By.css('dot-pages-favorite-panel'));
        elem.triggerEventHandler('showActionsMenu', actionMenuParam);

        expect(component.menu.hide).toHaveBeenCalledTimes(1);
        expect(store.showActionsMenu).toHaveBeenCalledWith({
            item: dotcmsContentletMock,
            actionMenuDomId: 'test1'
        });
    });

    it('should call goToUrl method from DotPagesListingPanel', () => {
        const elem = de.query(By.css('dot-pages-listing-panel'));
        elem.triggerEventHandler('goToUrl', '/page/1?lang=1');

        expect(store.setPortletStatus).toHaveBeenCalledWith(ComponentStatus.LOADING);
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

        const elem = de.query(By.css('dot-pages-listing-panel'));
        elem.triggerEventHandler('showActionsMenu', actionMenuParam);

        expect(component.menu.hide).toHaveBeenCalledTimes(1);
        expect(store.showActionsMenu).toHaveBeenCalledWith({
            item: dotcmsContentletMock,
            actionMenuDomId: 'test1'
        });
    });

    it('should call closedActionsMenu method from p-menu', () => {
        const elem = de.query(By.css('p-menu'));

        component.closedActionsMenu = jasmine.createSpy('closedActionsMenu');
        elem.triggerEventHandler('onHide', {});

        expect(component.closedActionsMenu).toHaveBeenCalledTimes(1);
    });

    it('should call push method in dotMessageDisplayService once a save-page is received for a non favorite page', () => {
        const dotEventsService: DotEventsService = de.injector.get(DotEventsService);

        dotEventsService.notify('save-page', { payload: { identifier: '123' }, value: 'test3' });

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
        const dotEventsService: DotEventsService = de.injector.get(DotEventsService);

        dotEventsService.notify('save-page', {
            payload: { contentType: 'dotFavoritePage', identifier: '123' },
            value: 'test3'
        });

        expect(store.updateSinglePageData).toHaveBeenCalledWith({
            identifier: '123',
            isFavoritePage: true
        });
    });

    it('should get pages after closing DotCreateContentletComponent', () => {
        const dialogComponentFixture = TestBed.createComponent(DotCreateContentletComponent);
        fixture.detectChanges();

        const dialogComponent = dialogComponentFixture.componentInstance;

        const routerOutlet = de.query(By.css('router-outlet'));

        routerOutlet.triggerEventHandler('activate', dialogComponent);
        fixture.detectChanges();

        dialogComponent.shutdown.emit();
        fixture.detectChanges();
        expect(store.getPages).toHaveBeenCalled();
    });
    it('should trigger unsubscribeToShutdown when deactivating the router-outlet', () => {
        const routerOutlet = de.query(By.css('router-outlet'));
        spyOn(component, 'unsubscribeToShutdown');

        routerOutlet.triggerEventHandler('activate');
        fixture.detectChanges();
        routerOutlet.triggerEventHandler('deactivate');
        fixture.detectChanges();

        expect(component.unsubscribeToShutdown).toHaveBeenCalled();
    });
});
