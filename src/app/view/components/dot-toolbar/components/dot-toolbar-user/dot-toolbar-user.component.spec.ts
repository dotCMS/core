import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotDropdownComponent } from '../../../_common/dot-dropdown-component/dot-dropdown.component';
import { DotMyAccountComponent } from '../dot-my-account/dot-my-account.component';
import { DotLoginAsComponent } from '../dot-login-as/dot-login-as.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from 'dotcms-js';
import { LoginServiceMock, mockAuth, mockUser } from '../../../../../test/login-service.mock';
import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { RouterTestingModule } from '@angular/router/testing';
import { BaseRequestOptions, ConnectionBackend, Http, Jsonp, RequestOptions } from '@angular/http';
import { LOCATION_TOKEN } from 'src/app/providers';
import { DotMenuService } from '@services/dot-menu.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotGravatarModule } from '../dot-gravatar/dot-gravatar.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { ButtonModule } from 'primeng/button';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { MockBackend } from '@angular/http/testing';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { FormatDateService } from '@services/format-date-service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('DotToolbarUserComponent', () => {
    let comp: DotToolbarUserComponent;
    let fixture: ComponentFixture<DotToolbarUserComponent>;
    let de: DebugElement;
    let dotDropdownComponent: DotDropdownComponent;
    let loginService: LoginService;
    let locationService: Location;
    let dotNavigationService: DotNavigationService;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotDropdownComponent,
                DotLoginAsComponent,
                DotMyAccountComponent,
                DotToolbarUserComponent
            ],
            providers: [
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload() {}
                    }
                },
                { provide: LoginService, useClass: LoginServiceMock },
                DotRouterService,
                IframeOverlayService,
                Jsonp,
                DotNavigationService,
                DotMenuService,
                LoggerService,
                StringUtils,
                DotEventsService,
                DotIframeService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                Http,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                UserModel,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                FormatDateService
            ],
            imports: [
                BrowserAnimationsModule,
                DotDialogModule,
                DotGravatarModule,
                DotIconButtonModule,
                DotIconModule,
                SearchableDropDownModule,
                RouterTestingModule,
                MdInputTextModule,
                ButtonModule,
                DotPipesModule,
                FormsModule,
                ReactiveFormsModule,
                PasswordModule,
                CheckboxModule,
                HttpClientTestingModule
            ]
        });

        fixture = TestBed.createComponent(DotToolbarUserComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
        dotNavigationService = de.injector.get(DotNavigationService);
        dotRouterService = de.injector.get(DotRouterService);
    });

    it('should call doLogOut on logout click', () => {
        comp.auth = {
            user: mockUser,
            loginAsUser: null
        };
        fixture.detectChanges();
        spyOn(dotRouterService, 'doLogOut');
        dotDropdownComponent = de.query(By.css('dot-dropdown-component')).componentInstance;
        dotDropdownComponent.onToggle();
        fixture.detectChanges();
        const logoutLink = de.query(By.css('#dot-toolbar-user-link-logout'));
        logoutLink.triggerEventHandler('click', {});
        expect(dotRouterService.doLogOut).toHaveBeenCalled();
    });

    it('should call "logoutAs" in "LoginService" on logout click', () => {
        comp.auth = mockAuth;
        spyOn(dotNavigationService, 'goToFirstPortlet').and.returnValue(
            new Promise(resolve => {
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
    });
});
