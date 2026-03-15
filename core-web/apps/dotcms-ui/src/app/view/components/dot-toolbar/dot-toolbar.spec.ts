/* eslint-disable @typescript-eslint/no-explicit-any */
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { Subject, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injectable, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ToolbarModule } from 'primeng/toolbar';

import {
    DotCurrentUserService,
    DotEventsService,
    DotEventsSocket,
    DotPropertiesService,
    DotRouterService,
    DotSiteService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotCurrentUserServiceMock, MockDotRouterService, mockSites } from '@dotcms/utils-testing';

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

describe('DotToolbarComponent', () => {
    let spectator: Spectator<DotToolbarComponent>;
    let dotRouterService: SpyObject<DotRouterService>;
    let dotPropertiesService: SpyObject<DotPropertiesService>;
    let iframeOverlayService: IframeOverlayService;
    let dotSiteService: SpyObject<DotSiteService>;
    let globalStore: SpyObject<InstanceType<typeof GlobalStore>>;

    const siteMock = mockSites[0];
    const switchSiteSubject = new Subject<DotSite>();

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
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            mockProvider(DotPropertiesService, {
                getFeatureFlag: jest.fn().mockImplementation(() => of(true))
            }),
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(siteMock)),
                switchSite: jest.fn().mockReturnValue(of({ hostSwitched: true })),
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
                getSiteById: jest
                    .fn()
                    .mockImplementation((id: string) =>
                        of(mockSites.find((s) => s.identifier === id) || siteMock)
                    )
            }),
            mockProvider(GlobalStore, {
                siteDetails: signal(siteMock),
                setCurrentSite: jest.fn(),
                switchSiteEvent$: jest.fn().mockReturnValue(switchSiteSubject.asObservable())
            }),
            mockProvider(DotEventsSocket, { on: jest.fn().mockReturnValue(new Subject()) }),
            { provide: DotNavigationService, useClass: MockDotNavigationService },
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { _routerState: { url: 'any/url' } } }
            },
            { provide: DotRouterService, useClass: MockDotRouterService },
            DotEventsService,
            IframeOverlayService,
            DotNavLogoService,
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
        dotSiteService = spectator.inject(DotSiteService);
        globalStore = spectator.inject(GlobalStore);
        jest.spyOn(spectator.component, 'siteChange');
        jest.spyOn(iframeOverlayService, 'show');
        jest.spyOn(iframeOverlayService, 'hide');
        dotPropertiesService.getFeatureFlag.mockReturnValue(of(true));
    });

    it(`should has a dot-crumbtrail`, () => {
        spectator.detectChanges();
        expect(spectator.query('dot-crumbtrail')).not.toBeNull();
    });

    it(`should has a dot-toolbar-notifications`, () => {
        spectator.detectChanges();
        expect(spectator.query('dot-toolbar-notifications')).not.toBeNull();
    });

    it(`should has a dot-toolbar-user`, () => {
        spectator.detectChanges();
        expect(spectator.query('dot-toolbar-user')).not.toBeNull();
    });

    it(`should has a dot-toolbar-announcements`, () => {
        dotPropertiesService.getFeatureFlag.mockReturnValue(of(true));
        spectator.detectChanges();
        expect(spectator.query('dot-toolbar-announcements')).not.toBeNull();
    });

    it(`should has not a dot-toolbar-announcements with feature flag disabled`, () => {
        dotPropertiesService.getFeatureFlag.mockReturnValue(of(false));
        spectator.detectChanges();
        expect(spectator.query('dot-toolbar-announcements')).toBeNull();
    });

    it(`should NOT go to site browser when site change in any portlet but edit page`, () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(false);
        spectator.detectChanges();
        spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith(siteMock.identifier);
        expect(dotSiteService.switchSite).toHaveBeenCalledWith(siteMock.identifier);
        expect(dotSiteService.getCurrentSite).toHaveBeenCalled();
        expect(globalStore.setCurrentSite).toHaveBeenCalledWith(siteMock);
    });

    it(`should go to site-browser when site change on edit page url`, () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(true);
        spectator.detectChanges();
        spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        expect<any>(spectator.component.siteChange).toHaveBeenCalledWith(siteMock.identifier);
        expect(dotSiteService.switchSite).toHaveBeenCalledWith(siteMock.identifier);
        expect(dotSiteService.getCurrentSite).toHaveBeenCalled();
        expect(globalStore.setCurrentSite).toHaveBeenCalledWith(siteMock);
    });

    it(`should call switchSite then getCurrentSite and setCurrentSite when site changes`, () => {
        spectator.detectChanges();
        dotSiteService.switchSite.mockClear();
        dotSiteService.getCurrentSite.mockClear();
        (globalStore.setCurrentSite as jest.Mock).mockClear();

        spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);

        expect(dotSiteService.switchSite).toHaveBeenCalledWith(siteMock.identifier);
        expect(dotSiteService.getCurrentSite).toHaveBeenCalled();
        expect(globalStore.setCurrentSite).toHaveBeenCalledWith(siteMock);
    });

    describe('SWITCH_SITE WebSocket event', () => {
        it('should update the store when SWITCH_SITE fires', () => {
            spectator.detectChanges();
            const newSite = mockSites[1];
            switchSiteSubject.next(newSite);
            expect(globalStore.setCurrentSite).toHaveBeenCalledWith(newSite);
        });

        it('should navigate to site browser when SWITCH_SITE fires on edit page', () => {
            jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(true);
            spectator.detectChanges();
            switchSiteSubject.next(mockSites[1]);
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        });

        it('should NOT navigate to site browser when SWITCH_SITE fires on non-edit page', () => {
            jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(false);
            spectator.detectChanges();
            switchSiteSubject.next(mockSites[1]);
            expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        });
    });

    describe('dot-site component integration', () => {
        it(`should render dot-site component with correct inputs and bindings`, () => {
            spectator.detectChanges();
            const siteComponent = spectator.query('dot-site');
            expect(siteComponent).not.toBeNull();
            expect(siteComponent).toHaveClass('w-64');

            expect(spectator.component.$currentSite()).toEqual(siteMock);
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
