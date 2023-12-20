/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { DotWizardService } from '@dotcms/app/api/services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotCurrentUserService,
    DotEventsService,
    DotLicenseService,
    DotRouterService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    LoginServiceMock,
    MockDotRouterService,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';

import { IFrameModule } from '../index';

const routeDatamock = {
    canAccessPortlet: true
};

class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }

    get parent() {
        return {
            url: of([
                {
                    path: 'an-url'
                }
            ])
        };
    }
}

xdescribe('IframePortletLegacyComponent', () => {
    let comp: IframePortletLegacyComponent;
    let fixture: ComponentFixture<IframePortletLegacyComponent>;
    let de: DebugElement;
    let dotIframe: DebugElement;
    let dotMenuService: DotMenuService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let route: ActivatedRoute;
    const siteServiceMock = new SiteServiceMock();

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [],
            imports: [
                IFrameModule,
                RouterTestingModule,
                DotDownloadBundleDialogModule,
                HttpClientTestingModule
            ],
            providers: [
                DotContentTypeService,
                DotCustomEventHandlerService,
                DotPushPublishDialogService,
                DotMenuService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                { provide: SiteService, useValue: siteServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotContentletEditorService,
                DotIframeService,
                DotWorkflowEventHandlerService,
                PushPublishService,
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                DotCurrentUserService,
                DotMessageDisplayService,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                DotFormatDateService,
                DotWizardService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService,
                DotLicenseService
            ]
        });

        fixture = TestBed.createComponent(IframePortletLegacyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotMenuService = de.injector.get(DotMenuService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);
        route = de.injector.get(ActivatedRoute);
    }));

    it('should set query param url to the dot-iframe src', () => {
        route.queryParams = of({ url: 'hello/world' });
        route.params = of({ id: 'portlet-id' });

        let src: string;
        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(src).toEqual('hello/world');
    });

    it('should set router param id to the dot-iframe src', () => {
        route.queryParams = of({});
        route.params = of({ id: 'portlet-id' });

        spyOn(dotMenuService, 'getUrlById').and.returnValue(of('fake-url'));

        let src: string;

        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(dotMenuService.getUrlById).toHaveBeenCalledWith('portlet-id');
        expect(src).toEqual('fake-url');
    });

    it('should handle custom events', () => {
        route.queryParams = of({ url: 'hello/world' });
        route.params = of({ id: 'portlet-id' });
        spyOn(dotCustomEventHandlerService, 'handle');
        fixture.detectChanges();

        dotIframe = de.query(By.css('dot-iframe'));

        dotIframe.triggerEventHandler('custom', {
            this: {
                is: 'a custom event'
            }
        });

        expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
            this: {
                is: 'a custom event'
            }
        });
    });

    it('should load Not Licensed component when no license and enterprise portlet ', () => {
        routeDatamock.canAccessPortlet = false;
        fixture.detectChanges();
        expect(de.query(By.css('dot-not-licensed-component'))).toBeTruthy();
    });

    it('should call reloadIframePortlet once', () => {
        fixture.detectChanges();
        comp.url.next('test');
        spyOn(comp, 'reloadIframePortlet');
        siteServiceMock.setFakeCurrentSite({
            identifier: '1',
            hostname: 'Site 1',
            archived: false,
            type: 'host'
        });
        siteServiceMock.setFakeCurrentSite({
            identifier: '2',
            hostname: 'Site 2',
            archived: false,
            type: 'host'
        });
        expect(comp.reloadIframePortlet).toHaveBeenCalledTimes(1);
    });
});
