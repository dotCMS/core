/* eslint-disable @typescript-eslint/no-explicit-any */
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ToolbarModule } from 'primeng/toolbar';

import { DotEventsService, DotPropertiesService, DotRouterService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    SiteService,
    StringUtils
} from '@dotcms/dotcms-js';
import { MockDotRouterService, mockSites, SiteServiceMock } from '@dotcms/utils-testing';

import { DotToolbarAnnouncementsComponent } from './components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotToolbarNotificationsComponent } from './components/dot-toolbar-notifications/dot-toolbar-notifications.component';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';
import { DotToolbarComponent } from './dot-toolbar.component';

import { DotNavLogoService } from '../../../api/services/dot-nav-logo/dot-nav-logo.service';
import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotSiteSelectorComponent } from '../_common/dot-site-selector/dot-site-selector.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotCrumbtrailComponent } from '../dot-crumbtrail/dot-crumbtrail.component';
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
    template: '',
    standalone: false
})
class MockToolbarUsersComponent {}

@Component({
    selector: 'dot-toolbar-notifications',
    template: '',
    standalone: false
})
class MockToolbarNotificationsComponent {}

@Component({
    selector: 'dot-toolbar-announcements',
    template: '',
    standalone: false
})
class MockToolbarAnnouncementsComponent {}

export const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

describe('DotToolbarComponent', () => {
    let spectator: Spectator<DotToolbarComponent>;
    let dotRouterService: SpyObject<DotRouterService>;
    let dotPropertiesService: SpyObject<DotPropertiesService>;

    const siteServiceMock = new SiteServiceMock();
    const siteMock = mockSites[0];

    const createComponent = createComponentFactory({
        component: DotToolbarComponent,
        imports: [ToolbarModule, DotShowHideFeatureDirective],
        detectChanges: false,
        declarations: [
            MockComponent(DotCrumbtrailComponent),
            MockComponent(DotSiteSelectorComponent)
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotPropertiesService, {
                getFeatureFlag: jest.fn().mockReturnValue(of(true))
            }),
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
        componentImports: [
            [DotToolbarUserComponent, MockToolbarUsersComponent],
            [DotToolbarNotificationsComponent, MockToolbarNotificationsComponent],
            [DotToolbarAnnouncementsComponent, MockToolbarAnnouncementsComponent]
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotRouterService = spectator.inject(DotRouterService);
        dotPropertiesService = spectator.inject(DotPropertiesService);
        jest.spyOn(spectator.component, 'siteChange');
    });

    it(`should has a dot-crumbtrail`, () => {
        spectator.detectChanges();

        const crumbtrail = spectator.query('dot-crumbtrail');
        expect(crumbtrail).not.toBeNull();
    });

    it(`should has a dot-toolbar-notifications`, () => {
        spectator.detectChanges();

        const dotToolbarNotifications = spectator.query('dot-toolbar-notifications');
        expect(dotToolbarNotifications).not.toBeNull();
    });

    it(`should has a dot-toolbar-user`, () => {
        spectator.detectChanges();

        const dotToolbarUser = spectator.query('dot-toolbar-user');
        expect(dotToolbarUser).not.toBeNull();
    });

    it(`should has a dot-toolbar-announcements`, () => {
        dotPropertiesService.getFeatureFlag.mockReturnValue(of(true));
        spectator.detectChanges();

        const dotToolbarAnnouncements = spectator.query('dot-toolbar-announcements');
        expect(dotToolbarAnnouncements).not.toBeNull();
    });

    it(`should has not a dot-toolbar-announcements with feature flag disabled`, () => {
        dotPropertiesService.getFeatureFlag.mockReturnValue(of(false));
        spectator.detectChanges();

        const dotToolbarAnnouncements = spectator.query('dot-toolbar-announcements');
        expect(dotToolbarAnnouncements).toBeNull();
    });

    it(`should NOT go to site browser when site change in any portlet but edit page`, () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(false);
        spectator.detectChanges();
        spectator.triggerEventHandler('dot-site-selector', 'switch', { value: siteMock });
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });

    it(`should go to site-browser when site change on edit page url`, () => {
        Object.defineProperty(dotRouterService, 'currentPortlet', {
            value: {
                id: 'edit-page',
                url: ''
            },
            writable: true
        });
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(true);
        spectator.detectChanges();
        spectator.triggerEventHandler('dot-site-selector', 'switch', { value: siteMock });

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });

    it(`should pass class and width`, () => {
        spectator.detectChanges();
        const siteSelector = spectator.query('dot-site-selector');
        expect(siteSelector.getAttribute('cssClass')).toContain('d-secondary');
        expect(siteSelector.getAttribute('width')).toContain('12.5rem');
    });
});
