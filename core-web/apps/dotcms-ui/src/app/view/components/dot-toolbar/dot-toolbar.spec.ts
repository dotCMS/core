/* eslint-disable @typescript-eslint/no-explicit-any */
import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ToolbarModule } from 'primeng/toolbar';

import { DotSiteSelectorComponent } from '@components/_common/dot-site-selector/dot-site-selector.component';
import { DotCrumbtrailComponent } from '@components/dot-crumbtrail/dot-crumbtrail.component';
import { DotNavLogoService } from '@dotcms/app/api/services/dot-nav-logo/dot-nav-logo.service';
import { DotEventsService, DotRouterService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    SiteService,
    LoggerService,
    StringUtils
} from '@dotcms/dotcms-js';
import { MockDotRouterService, mockSites, SiteServiceMock } from '@dotcms/utils-testing';

import { DotToolbarNotificationsComponent } from './components/dot-toolbar-notifications/dot-toolbar-notifications.component';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';
import { DotToolbarComponent } from './dot-toolbar.component';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';

@Injectable()
class MockDotNavigationService {
    collapsed = false;

    toggle() {
        this.collapsed = !this.collapsed;
    }
}

@Component({
    selector: 'dot-toolbar-user',
    template: ''
})
class MockToolbarUsersComponent {}

export const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

xdescribe('DotToolbarComponent', () => {
    let spectator: Spectator<DotToolbarComponent>;
    let dotRouterService: SpyObject<DotRouterService>;

    const siteServiceMock = new SiteServiceMock();
    const siteMock = mockSites[0];

    const createComponent = createComponentFactory({
        component: DotToolbarComponent,
        imports: [ToolbarModule],
        detectChanges: false,
        declarations: [
            MockComponent(DotToolbarNotificationsComponent),
            MockComponent(DotCrumbtrailComponent),
            MockComponent(DotSiteSelectorComponent)
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotNavigationService, useClass: MockDotNavigationService },
            { provide: SiteService, useValue: siteServiceMock },
            mockProvider(ActivatedRoute, {
                snapshot: {
                    _routerState: {
                        url: 'any/url'
                    }
                }
            }),
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotRouterService, useClass: MockDotRouterService },
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            DotEventsService,
            DotcmsEventsService,
            IframeOverlayService,
            DotNavLogoService,
            DotEventsSocket,
            DotcmsConfigService,
            LoggerService,
            StringUtils
        ],
        componentImports: [[DotToolbarUserComponent, MockToolbarUsersComponent]]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotRouterService = spectator.inject(DotRouterService);
        spyOn(spectator.component, 'siteChange').and.callThrough();
    });

    it(`should has a crumbtrail`, () => {
        spectator.detectChanges();

        const crumbtrail = spectator.query('dot-crumbtrail');
        expect(crumbtrail).not.toBeNull();
    });

    it(`should NOT go to site browser when site change in any portlet but edit page`, () => {
        spyOn(dotRouterService, 'isEditPage').and.returnValue(false);
        spectator.detectChanges();
        spectator.triggerEventHandler('dot-site-selector', 'switch', { value: siteMock });
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });

    it(`should go to site-browser when site change on edit page url`, () => {
        spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
            id: 'edit-page',
            url: ''
        });
        spyOn(dotRouterService, 'isEditPage').and.returnValue(true);
        spectator.triggerEventHandler('dot-site-selector', 'switch', { value: siteMock });

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });

    it(`should pass class and width`, () => {
        const siteSelector = spectator.query('dot-site-selector');
        expect(siteSelector.getAttribute('cssClass')).toBe('d-secondary');
        expect(siteSelector.getAttribute('width')).toBe('200px');
    });
});
