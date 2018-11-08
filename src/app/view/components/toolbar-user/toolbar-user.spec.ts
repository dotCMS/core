import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
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
import { DebugElement } from '@angular/core';
import { async } from '@angular/core/testing';

import { LoginService } from 'dotcms-js';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { LoginServiceMock, mockAuth } from '../../../test/login-service.mock';
import { ToolbarUserComponent } from './toolbar-user';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { RouterTestingModule } from '@angular/router/testing';
import { Jsonp } from '@angular/http';
import { LOCATION_TOKEN } from 'src/app/providers';

describe('ToolbarUserComponent', () => {
    let comp: ToolbarUserComponent;
    let fixture: ComponentFixture<ToolbarUserComponent>;
    let de: DebugElement;
    let dotDropdownComponent: DotDropdownComponent;
    let loginService: LoginService;
    let locationService: Location;

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
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload() {}
                    }
                },
                { provide: LoginService, useClass: LoginServiceMock },
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
                DotDialogModule,
                RouterTestingModule
            ]
        });

        fixture = DOTTestBed.createComponent(ToolbarUserComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
    }));

    it('should call "logoutAs" in "LoginService" on logout click', () => {
        spyOn(locationService, 'reload');
        spyOn(loginService, 'logoutAs').and.callThrough();
        comp.auth = mockAuth;
        fixture.detectChanges();

        dotDropdownComponent = de.query(By.css('dot-dropdown-component')).componentInstance;
        dotDropdownComponent.onToggle();
        fixture.detectChanges();

        const logoutAsLink = de.query(By.css('#dot-toolbar-user-link-logout-as'));
        logoutAsLink.nativeElement.click();

        expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    });
});
