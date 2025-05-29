/* eslint-disable @typescript-eslint/no-explicit-any */
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, Injectable } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

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
    standalone: true,
    template: ''
})
class MockToolbarUsersComponent {}

export const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

describe('DotToolbarComponent', () => {
    let dotRouterService: DotRouterService;
    let dotNavigationService: DotNavigationService;
    let dotNavLogoService: DotNavLogoService;
    let comp: DotToolbarComponent;
    let fixture: ComponentFixture<DotToolbarComponent>;
    let de: DebugElement;

    const siteServiceMock = new SiteServiceMock();
    const siteMock = mockSites[0];

    let spectator: Spectator<DotToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotToolbarComponent,
        detectChanges: false,
        declarations: [
            MockComponent(DotToolbarNotificationsComponent),
            MockComponent(DotCrumbtrailComponent)
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

        fixture = spectator.fixture;
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotRouterService = spectator.inject(DotRouterService);
        dotNavigationService = spectator.inject(DotNavigationService);
        dotNavLogoService = spectator.inject(DotNavLogoService);
        spyOn(comp, 'siteChange').and.callThrough();
    });

    it(`should has a crumbtrail`, () => {
        fixture.detectChanges();

        const crumbtrail: DebugElement = fixture.debugElement.query(By.css('dot-crumbtrail'));
        expect(crumbtrail).not.toBeNull();
    });

    it(`should NOT go to site browser when site change in any portlet but edit page`, () => {
        spyOn(dotRouterService, 'isEditPage').and.returnValue(false);
        const siteSelector: DebugElement = fixture.debugElement.query(By.css('dot-site-selector'));
        fixture.detectChanges();
        siteSelector.triggerEventHandler('switch', { value: siteMock });
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
        expect<any>(comp.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });

    it(`should go to site-browser when site change on edit page url`, () => {
        const siteSelector: DebugElement = fixture.debugElement.query(By.css('dot-site-selector'));
        spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
            id: 'edit-page',
            url: ''
        });
        spyOn(dotRouterService, 'isEditPage').and.returnValue(true);
        siteSelector.triggerEventHandler('switch', { value: siteMock });

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        expect<any>(comp.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });

    it(`should pass class and width`, () => {
        const siteSelector: DebugElement = fixture.debugElement.query(By.css('dot-site-selector'));
        expect(siteSelector.componentInstance.cssClass).toBe('d-secondary');
        expect(siteSelector.componentInstance.width).toBe('200px');
    });

    it('should toggle menu and update icon on click', () => {
        spyOn(dotNavigationService, 'toggle').and.callThrough();
        fixture.detectChanges();

        const button: DebugElement = de.query(By.css('p-button'));

        expect(button.componentInstance.icon).toEqual('pi pi-arrow-left');
        button.triggerEventHandler('click', {});
        fixture.detectChanges();

        expect(dotNavigationService.toggle).toHaveBeenCalledTimes(1);
        expect(button.componentInstance.icon).toEqual('pi pi-arrow-left');
    });

    it('should have default logo', () => {
        dotNavLogoService.navBarLogo$.next(null);
        fixture.detectChanges();
        const defaultLogo = de.nativeElement.querySelector('.toolbar__logo');
        expect(defaultLogo).not.toBeNull();
    });

    it('should have the logo passed to the subject', () => {
        const imageUrlProp = 'url("/dA/image.png")';
        dotNavLogoService.navBarLogo$.next(imageUrlProp);
        fixture.detectChanges();
        const newLogo = de.nativeElement.querySelector('.toolbar__logo--whitelabel');
        expect(newLogo.style['background-image']).toBe(imageUrlProp);
        expect(newLogo).not.toBeNull();
    });
});
