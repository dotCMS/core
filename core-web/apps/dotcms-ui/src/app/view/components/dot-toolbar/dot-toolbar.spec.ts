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
    DotSystemConfigService
} from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotCurrentUserServiceMock, MockDotRouterService, mockSites } from '@dotcms/utils-testing';

import { DotToolbarAnnouncementsComponent } from './components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotToolbarNotificationsComponent } from './components/dot-toolbar-notifications/dot-toolbar-notifications.component';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';
import { DotToolbarComponent } from './dot-toolbar.component';

import { DotNavLogoService } from '../../../api/services/dot-nav-logo/dot-nav-logo.service';
import { DotSiteNavigationEffect } from '../../../api/services/dot-site-navigation/dot-site-navigation.effect';
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
    let globalStore: SpyObject<InstanceType<typeof GlobalStore>>;

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
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            mockProvider(DotPropertiesService, {
                getFeatureFlag: jest.fn().mockImplementation(() => of(true))
            }),
            mockProvider(GlobalStore, {
                siteDetails: signal(siteMock),
                switchCurrentSite: jest.fn(),
                switchSiteEvent$: jest.fn().mockReturnValue(new Subject())
            }),
            mockProvider(DotSiteNavigationEffect),
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

    describe('siteChange()', () => {
        it(`should call switchCurrentSite and NOT navigate when not on edit page`, () => {
            jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(false);
            spectator.detectChanges();
            spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);

            expect<any>(spectator.component.siteChange).toHaveBeenCalledWith(siteMock.identifier);
            expect(globalStore.switchCurrentSite).toHaveBeenCalledWith(siteMock.identifier);
            expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        });

        it(`should call switchCurrentSite and navigate when on edit page`, () => {
            jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(true);
            spectator.detectChanges();
            spectator.triggerEventHandler('dot-site', 'onChange', siteMock.identifier);

            expect<any>(spectator.component.siteChange).toHaveBeenCalledWith(siteMock.identifier);
            expect(globalStore.switchCurrentSite).toHaveBeenCalledWith(siteMock.identifier);
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
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
