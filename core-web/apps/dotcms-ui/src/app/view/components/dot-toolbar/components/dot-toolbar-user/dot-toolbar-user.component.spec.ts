/* eslint-disable @typescript-eslint/no-empty-function */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { PasswordModule } from 'primeng/password';

import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotGravatarDirective } from '@directives/dot-gravatar/dot-gravatar.directive';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@dotcms/app/test/dot-test-bed';
import { DotEventsService } from '@dotcms/data-access';
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
import { DotIconModule, DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { CoreWebServiceMock, LoginServiceMock } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { DotToolbarUserStore } from './store/dot-toolbar-user.store';

import { DotLoginAsModule } from '../dot-login-as/dot-login-as.module';
import { DotMyAccountModule } from '../dot-my-account/dot-my-account.module';

class DotGravatarServiceMock {
    getPhoto() {
        return of('/some_avatar_url');
    }
}

describe('DotToolbarUserComponent', () => {
    let fixture: ComponentFixture<DotToolbarUserComponent>;
    let de: DebugElement;
    let loginService: LoginService;
    let locationService: Location;
    let dotNavigationService: DotNavigationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload() {}
                    }
                },
                { provide: LoginService, useClass: LoginServiceMock },
                DotRouterService,
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
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                DotFormatDateService,
                { provide: DotGravatarService, useClass: DotGravatarServiceMock },
                DotToolbarUserStore
            ],
            imports: [
                BrowserAnimationsModule,
                DotDialogModule,
                UiDotIconButtonModule,
                DotIconModule,
                SearchableDropDownModule,
                RouterTestingModule,
                ButtonModule,
                DotPipesModule,
                DotMessagePipe,
                FormsModule,
                ReactiveFormsModule,
                PasswordModule,
                CheckboxModule,
                HttpClientTestingModule,
                MenuModule,
                DotLoginAsModule,
                DotMyAccountModule,
                DotToolbarUserComponent,
                DotGravatarDirective,
                AvatarModule
            ]
        });

        const mockDate = new Date(1466424490000);
        jasmine.clock().install();
        jasmine.clock().mockDate(mockDate);

        fixture = TestBed.createComponent(DotToolbarUserComponent);

        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
        dotNavigationService = de.injector.get(DotNavigationService);
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    it('should have correct href in logout link', () => {
        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutLink = de.query(By.css('#dot-toolbar-user-link-logout'));
        expect(logoutLink.attributes.href).toBe('/dotAdmin/logout?r=1466424490000');
    });
    it('should have correct target in logout link', () => {
        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutLink = de.query(By.css('#dot-toolbar-user-link-logout'));
        expect(logoutLink.attributes.target).toBe('_self');
    });

    it('should call "logoutAs" in "LoginService" on logout click', async () => {
        spyOn(dotNavigationService, 'goToFirstPortlet').and.returnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );
        spyOn(locationService, 'reload');
        spyOn(loginService, 'logoutAs').and.callThrough();

        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutAsLink = de.query(By.css('#dot-toolbar-user-link-logout-as'));
        logoutAsLink.triggerEventHandler('click', {
            preventDefault: () => {
                //
            }
        });

        await fixture.whenStable();
        expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    });

    it('should hide login as link', () => {
        spyOn(loginService, 'getCurrentUser').and.returnValue(
            of({
                email: 'admin@dotcms.com',
                givenName: 'Admin',
                loginAs: false,
                roleId: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
                surname: 'User',
                userId: 'dotcms.org.1'
            })
        );

        fixture.detectChanges();

        const loginAsLink = de.query(By.css('[data-testId="login-as"]'));
        expect(loginAsLink).toBe(null);
    });
});
