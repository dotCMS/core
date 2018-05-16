import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Injectable, Component, Input } from '@angular/core';
import { ToolbarComponent } from './dot-toolbar.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { SiteService } from 'dotcms-js/dotcms-js';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/dot-navigation.service';
import { SiteServiceMock, mockSites } from '../../../test/site-service.mock';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';

@Injectable()
class MockDotNavigationService {}

@Component({
    selector: 'dot-site-selector',
    template: ''
})
class MockSiteSelectorComponent {
    @Input() archive = false;
    @Input() id = '';
    @Input() live = true;
    @Input() system = true;
}

@Component({
    selector: 'dot-global-message',
    template: ''
})
class MockGlobalMessageComponent {}

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

@Injectable()
class MockDotRouterService {
    goToSiteBrowser() {}
}

describe('ToolbarComponent', () => {
    let dotRouterService: DotRouterService;
    let siteService: SiteService;
    let comp: ToolbarComponent;
    let fixture: ComponentFixture<ToolbarComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    const siteServiceMock = new SiteServiceMock();
    const siteMock = mockSites[0];

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                ToolbarComponent,
                MockSiteSelectorComponent,
                MockGlobalMessageComponent,
                MockToolbarNotificationsComponent,
                MockToolbarUsersComponent,
                MockToolbarAddContentletComponent
            ],
            imports: [BrowserAnimationsModule],
            providers: [
                { provide: DotNavigationService, useClass: MockDotNavigationService },
                { provide: SiteService, useValue: siteServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                IframeOverlayService
            ]
        });

        fixture = DOTTestBed.createComponent(ToolbarComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
        dotRouterService = fixture.debugElement.injector.get(DotRouterService);
        siteService = fixture.debugElement.injector.get(SiteService);
    }));

    it('should trigger "siteChange" call "goToSiteBrowser" in "DotRouterService" when the "siteChange" method is actioned', () => {
        const siteSelector: DebugElement = fixture.debugElement.query(By.css('dot-site-selector'));
        spyOn(comp, 'siteChange').and.callThrough();
        spyOn(dotRouterService, 'goToSiteBrowser');
        siteSelector.triggerEventHandler('change', { value: siteMock });
        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
        expect(comp.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });
});
