import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotDropdownComponent } from '../../../_common/dot-dropdown-component/dot-dropdown.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { async } from '@angular/core/testing';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from 'dotcms-js';
import { LoginServiceMock, mockAuth } from '../../../../../test/login-service.mock';
import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { RouterTestingModule } from '@angular/router/testing';
import { Jsonp } from '@angular/http';
import { LOCATION_TOKEN } from 'src/app/providers';
import { DotMenuService } from '@services/dot-menu.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { ButtonModule } from 'primeng/primeng';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { FormatDateService } from '@services/format-date-service';

@Component({
    selector:
        '<dot-login-as [visible]="showLoginAs" (cancel)="tooggleLoginAs($event)"></dot-login-as>',
    template: ''
})
class MockLoginAsComponent {
    @Input() visible: boolean;
    @Output() cancel = new EventEmitter<any>();
}

@Component({
    selector:
        '<dot-my-account [visible]="showMyAccount" (cancel)="toggleMyAccount()"></dot-my-account>',
    template: ''
})
class MockMyAccountComponent {
    @Input() visible: boolean;
    @Output() cancel = new EventEmitter<any>();
}

@Component({
    selector: '<dot-gravatar [email]="test""></dot-gravatar>',
    template: ''
})
class MockGravatarComponent {
    @Input() email: string;
}

describe('DotToolbarUserComponent', () => {
    let comp: DotToolbarUserComponent;
    let fixture: ComponentFixture<DotToolbarUserComponent>;
    let de: DebugElement;
    let dotDropdownComponent: DotDropdownComponent;
    let loginService: LoginService;
    let locationService: Location;
    let dotNavigationService: DotNavigationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                ButtonModule,
                BrowserAnimationsModule,
                DotDialogModule,
                DotIconButtonModule,
                DotIconModule,
                SearchableDropDownModule,
                RouterTestingModule,
                MdInputTextModule,
                DotPipesModule,
                HttpClientTestingModule
            ],
            declarations: [
                DotDropdownComponent,
                MockLoginAsComponent,
                MockMyAccountComponent,
                DotToolbarUserComponent,
                MockGravatarComponent
            ],
            providers: [
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload() {}
                    }
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                IframeOverlayService,
                DotcmsEventsService,
                Jsonp,
                DotNavigationService,
                DotMenuService,
                LoggerService,
                StringUtils,
                DotEventsService,
                DotIframeService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                FormatDateService
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotToolbarUserComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
        dotNavigationService = de.injector.get(DotNavigationService);

        comp.auth = mockAuth;
    });

    it('should call "logoutAs" in "LoginService" on logout click', async(() => {
        spyOn(dotNavigationService, 'goToFirstPortlet').and.returnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );
        spyOn(locationService, 'reload');
        spyOn(loginService, 'logoutAs').and.callThrough();

        fixture.detectChanges();

        dotDropdownComponent = de.query(By.css('dot-dropdown-component')).componentInstance;
        dotDropdownComponent.onToggle();
        fixture.detectChanges();

        const logoutAsLink = de.query(By.css('#dot-toolbar-user-link-logout-as'));
        logoutAsLink.triggerEventHandler('click', {
            preventDefault: () => {}
        });

        fixture.whenStable().then(() => {
            expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
            expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
            expect(locationService.reload).toHaveBeenCalledTimes(1);
        });
    }));

    afterEach(() => {
        // Removes dirty DOM after tests have finished
        if (fixture.nativeElement && 'remove' in fixture.nativeElement) {
            (fixture.nativeElement as HTMLElement).remove();
        }
    });
});
