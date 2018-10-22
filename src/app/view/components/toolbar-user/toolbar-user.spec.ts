import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Jsonp } from '@angular/http';
import { GravatarService } from './../../../api/services/gravatar-service';
import { IframeOverlayService } from './../_common/iframe/service/iframe-overlay.service';
import { DataListModule, OverlayPanelModule } from 'primeng/primeng';
import { MaterialDesignTextfieldDirective } from './../../directives/md-inputtext/md-input-text.directive';
import { SearchableDropdownComponent } from './../_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotDropdownComponent } from './../_common/dropdown-component/dot-dropdown.component';
import { MyAccountComponent } from './../my-account/dot-my-account-component';
import { LoginAsComponent } from './../login-as/login-as';
import { GravatarComponent } from './../_common/gravatar/gravatar.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Injectable } from '@angular/core';
import { async } from '@angular/core/testing';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { LoginServiceMock, mockAuth } from '../../../test/login-service.mock';
import { ToolbarUserComponent } from './toolbar-user';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet() {}
}
describe('ToolbarUserComponent', () => {
    let comp: ToolbarUserComponent;
    let fixture: ComponentFixture<ToolbarUserComponent>;
    let de: DebugElement;
    let dotDropdownComponent: DotDropdownComponent;
    let loginService: LoginService;
    let dotEventsService: DotEventsService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DotDropdownComponent,
                GravatarComponent,
                LoginAsComponent,
                MyAccountComponent,
                SearchableDropdownComponent,
                ToolbarUserComponent,
                MaterialDesignTextfieldDirective
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotNavigationService, useClass: MockDotNavigationService },
                IframeOverlayService,
                GravatarService,
                Jsonp
            ],
            imports: [
                DataListModule,
                OverlayPanelModule,
                BrowserAnimationsModule,
                DotIconModule,
                DotIconButtonModule,
                DotDialogModule
            ]
        });

        fixture = DOTTestBed.createComponent(ToolbarUserComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        dotEventsService = de.injector.get(DotEventsService);
    }));

    it('should call "logoutAs" in "LoginService" when logout as happen', () => {
        spyOn(loginService, 'logoutAs').and.callThrough();
        spyOn(dotEventsService, 'notify');
        comp.auth = mockAuth;
        fixture.detectChanges();
        dotDropdownComponent = de.query(By.css('dot-dropdown-component')).componentInstance;
        dotDropdownComponent.onToggle();
        fixture.detectChanges();
        const logoutAsLink = de.query(By.css('#dot-toolbar-user-link-logout-as'));
        logoutAsLink.nativeElement.click();
        expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
        expect(dotEventsService.notify).toHaveBeenCalledWith('logout-as');
    });
});
