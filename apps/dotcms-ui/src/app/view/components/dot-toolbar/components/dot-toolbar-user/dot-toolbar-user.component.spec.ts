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
} from '@dotcms/dotcms-js';
import { LoginServiceMock, mockAuth, mockUser } from '../../../../../test/login-service.mock';
import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@dotcms/ui';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { RouterTestingModule } from '@angular/router/testing';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotMenuService } from '@services/dot-menu.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotGravatarModule } from '../dot-gravatar/dot-gravatar.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { ButtonModule } from 'primeng/button';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { of } from 'rxjs';

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
                DotcmsEventsService,
                DotNavigationService,
                DotMenuService,
                LoggerService,
                StringUtils,
                DotEventsService,
                DotIframeService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                UserModel,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                DotFormatDateService
            ],
            imports: [
                BrowserAnimationsModule,
                DotDialogModule,
                DotGravatarModule,
                UiDotIconButtonModule,
                DotIconModule,
                SearchableDropDownModule,
                RouterTestingModule,
                ButtonModule,
                DotPipesModule,
                FormsModule,
                ReactiveFormsModule,
                PasswordModule,
                CheckboxModule,
                HttpClientTestingModule
            ]
        });

        const mockDate = new Date(1466424490000);
        jasmine.clock().install();
        jasmine.clock().mockDate(mockDate);

        fixture = TestBed.createComponent(DotToolbarUserComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
        dotNavigationService = de.injector.get(DotNavigationService);
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    it('should have correct href in logout link', () => {
        comp.auth = {
            user: mockUser(),
            loginAsUser: null
        };
        fixture.detectChanges();

        dotDropdownComponent = de.query(By.css('dot-dropdown-component')).componentInstance;
        dotDropdownComponent.onToggle();
        fixture.detectChanges();

        const logoutLink = de.query(By.css('#dot-toolbar-user-link-logout'));
        expect(logoutLink.attributes.href).toBe('/dotAdmin/logout?r=1466424490000');
    });

    it('should call "logoutAs" in "LoginService" on logout click', async () => {
        comp.auth = mockAuth;
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

        await fixture.whenStable();
        expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    });

    it('should hide login as link', () => {
        comp.auth = mockAuth;
        spyOn(loginService, 'getCurrentUser').and.returnValue(of({
            email: 'admin@dotcms.com',
            givenName: 'Admin',
            loginAs: false,
            roleId: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
            surname: 'User',
            userId: 'dotcms.org.1'
        }));

        fixture.detectChanges();

        const loginAsLink = de.query(By.css('[data-testId="login-as"]'));
        expect(loginAsLink).toBe(null);
    })
});
