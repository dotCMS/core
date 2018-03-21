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
import { DebugElement } from '@angular/core';
import { async } from '@angular/core/testing';

import { LoginService } from 'dotcms-js/core/login.service';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';
import { LoginServiceMock, mockAuth } from '../../../test/login-service.mock';
import { ToolbarUserComponent } from './toolbar-user';

describe('ToolbarUserComponent', () => {
    let comp: ToolbarUserComponent;
    let fixture: ComponentFixture<ToolbarUserComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let dotIframeService: DotIframeService;
    let dotDropdownComponent: DotDropdownComponent;

    beforeEach(
        async(() => {
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
                providers: [{ provide: LoginService, useClass: LoginServiceMock }, IframeOverlayService, GravatarService, Jsonp],
                imports: [DataListModule, OverlayPanelModule, BrowserAnimationsModule]
            });

            fixture = DOTTestBed.createComponent(ToolbarUserComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;

            dotIframeService = de.injector.get(DotIframeService);
            spyOn(dotIframeService, 'reload');
        })
    );

    it('should call reload on iframe service when logout as happen', () => {
        comp.auth = mockAuth;
        fixture.detectChanges();

        dotDropdownComponent = de.query(By.css('dot-dropdown-component')).componentInstance;
        dotDropdownComponent.onToggle();
        fixture.detectChanges();

        const logoutAsLink = de.query(By.css('#dot-toolbar-user-link-logout-as'));
        logoutAsLink.nativeElement.click();

        expect(dotIframeService.reload).toHaveBeenCalledTimes(1);
    });
});
