/* eslint-disable @typescript-eslint/no-explicit-any */

import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DebugElement, Injectable, Component, Input } from '@angular/core';
import { DotToolbarComponent } from './dot-toolbar.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { SiteService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
import { SiteServiceMock, mockSites } from '../../../test/site-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { UiDotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@dotcms/ui';
import { DotNavLogoService } from '@services/dot-nav-logo/dot-nav-logo.service';

@Injectable()
class MockDotNavigationService {
    collapsed = false;

    toggle() {
        this.collapsed = !this.collapsed;
    }
}

@Injectable()
class MockRouterService {
    get snapshot() {
        return {
            _routerState: {
                url: 'any/url'
            }
        };
    }
}

@Component({
    selector: 'dot-site-selector',
    template: ''
})
class MockSiteSelectorComponent {
    @Input()
    archive = false;

    @Input()
    id = '';

    @Input()
    live = true;

    @Input()
    system = true;

    @Input()
    cssClass;

    @Input()
    width;
}

@Component({
    selector: 'dot-toolbar-notifications',
    template: ''
})
class MockToolbarNotificationsComponent {}

@Component({
    selector: 'dot-toolbar-user',
    template: ''
})
class MockToolbarUsersComponent {}

@Component({
    selector: 'dot-toolbar-add-contentlet',
    template: ''
})
class MockToolbarAddContentletComponent {}

@Component({
    selector: 'dot-crumbtrail',
    template: ''
})
class MockDotCrumbtrailComponent {}

describe('DotToolbarComponent', () => {
    let dotRouterService: DotRouterService;
    let dotNavigationService: DotNavigationService;
    let dotNavLogoService: DotNavLogoService;
    let comp: DotToolbarComponent;
    let fixture: ComponentFixture<DotToolbarComponent>;
    let de: DebugElement;

    const siteServiceMock = new SiteServiceMock();
    const siteMock = mockSites[0];

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [
                    DotToolbarComponent,
                    MockSiteSelectorComponent,
                    MockToolbarNotificationsComponent,
                    MockToolbarUsersComponent,
                    MockToolbarAddContentletComponent,
                    MockDotCrumbtrailComponent
                ],
                imports: [
                    BrowserAnimationsModule,
                    RouterTestingModule,
                    DotIconModule,
                    UiDotIconButtonModule
                ],
                providers: [
                    { provide: DotNavigationService, useClass: MockDotNavigationService },
                    { provide: SiteService, useValue: siteServiceMock },
                    { provide: ActivatedRoute, useClass: MockRouterService },
                    IframeOverlayService,
                    DotNavLogoService
                ]
            });

            fixture = DOTTestBed.createComponent(DotToolbarComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            dotRouterService = de.injector.get(DotRouterService);
            dotNavigationService = de.injector.get(DotNavigationService);
            dotNavLogoService = TestBed.inject(DotNavLogoService);
            spyOn(comp, 'siteChange').and.callThrough();
        })
    );

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

        const button: DebugElement = de.query(By.css('dot-icon-button'));

        expect(button.componentInstance.icon).toEqual('arrow_back');
        button.triggerEventHandler('click', {});
        fixture.detectChanges();

        expect(dotNavigationService.toggle).toHaveBeenCalledTimes(1);
        expect(button.componentInstance.icon).toEqual('arrow_back');
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
