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
import { RouterTestingModule } from '../../../../../node_modules/@angular/router/testing';

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet() {}
}

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

describe('ToolbarComponent', () => {
    let dotNavigationService: DotNavigationService;
    let comp: ToolbarComponent;
    let fixture: ComponentFixture<ToolbarComponent>;
    let de: DebugElement;

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
            imports: [BrowserAnimationsModule, RouterTestingModule],
            providers: [
                { provide: DotNavigationService, useClass: MockDotNavigationService },
                { provide: SiteService, useValue: siteServiceMock },
                IframeOverlayService
            ]
        });

        fixture = DOTTestBed.createComponent(ToolbarComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotNavigationService = fixture.debugElement.injector.get(DotNavigationService);
    }));

    it('should trigger "siteChange" call "goToSiteBrowser" in "DotRouterService" when the "siteChange" method is actioned', () => {
        const siteSelector: DebugElement = fixture.debugElement.query(By.css('dot-site-selector'));
        spyOn(comp, 'siteChange').and.callThrough();
        spyOn(dotNavigationService, 'goToFirstPortlet');
        siteSelector.triggerEventHandler('change', { value: siteMock });
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(comp.siteChange).toHaveBeenCalledWith({ value: siteMock });
    });
});
