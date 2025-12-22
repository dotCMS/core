/* eslint-disable @typescript-eslint/no-explicit-any */
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ToolbarModule } from 'primeng/toolbar';

import { DotEventsService, DotPropertiesService, DotRouterService, DotSiteService, DotSystemConfigService } from '@dotcms/data-access';
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
    template: ''
})
class MockToolbarUsersComponent {}

@Component({
    selector: 'dot-toolbar-notifications',
    template: ''
})
class MockToolbarNotificationsComponent {}

@Component({
    selector: 'dot-toolbar-announcements',
    template: ''
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
    let iframeOverlayService: IframeOverlayService;

    const siteServiceMock = new SiteServiceMock();
    const siteMock = mockSites[0];

    const createComponent = createComponentFactory({
        component: DotToolbarComponent,
        imports: [
            DotToolbarComponent,
            ToolbarModule,
            DotShowHideFeatureDirective,
            MockComponent(DotCrumbtrailComponent)
        ],
        detectChanges: false,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotPropertiesService, {
                getFeatureFlag: jest.fn().mockImplementation(() => of(true))
            }),
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(siteMock)),
                switchSite: jest.fn().mockReturnValue(of(siteMock)),
                getSites: jest.fn().mockReturnValue(
                    of({
                        sites: mockSites,
                        pagination: {
                            currentPage: 1,
                            perPage: 10,
                            totalRecords: mockSites.length
                        }
                    })
                ),
                getSiteById: jest.fn().mockImplementation((id: string) =>
                    of(mockSites.find((s) => s.identifier === id) || siteMock)
                )
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
            StringUtils,
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            }
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
        iframeOverlayService = spectator.inject(IframeOverlayService);
        jest.spyOn(spectator.component, 'siteChange');
        jest.spyOn(iframeOverlayService, 'show');
        jest.spyOn(iframeOverlayService, 'hide');
        // Reset feature flag mock to return true by default
        dotPropertiesService.getFeatureFlag.mockReturnValue(of(true));
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
        spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith(siteMock.identifier);
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
        spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith(siteMock.identifier);
    });

    describe('dot-site component integration', () => {
        it(`should render dot-site component with correct inputs and bindings`, () => {
            spectator.detectChanges();
            const siteComponent = spectator.query('dot-site');
            expect(siteComponent).not.toBeNull();
            expect(siteComponent).toHaveClass('w-64');

            // Verify that value is bound to current site identifier
            const componentInstance = spectator.component;
            expect(componentInstance.$currentSite()).toEqual(siteMock);
        });

        it(`should call iframeOverlayService.show() when dot-site onShow event is triggered`, () => {
            spectator.detectChanges();
            spectator.triggerEventHandler('dot-site', 'onShow', null);

            expect(iframeOverlayService.show).toHaveBeenCalled();
        });

        it(`should call iframeOverlayService.hide() when dot-site onHide event is triggered`, () => {
            spectator.detectChanges();
            spectator.triggerEventHandler('dot-site', 'onHide', null);

            expect(iframeOverlayService.hide).toHaveBeenCalled();
        });
    });
});
