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

import {
    DotEventsService,
    DotFormatDateService,
    DotIframeService,
    DotRouterService
} from '@dotcms/data-access';
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
import {
    DotDialogModule,
    DotGravatarDirective,
    DotIconModule,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { CoreWebServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { DotToolbarUserStore } from './store/dot-toolbar-user.store';

import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotUiColorsService } from '../../../../../api/services/dot-ui-colors/dot-ui-colors.service';
import { LOCATION_TOKEN } from '../../../../../providers';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../../../test/dot-test-bed';
import { SearchableDropDownModule } from '../../../_common/searchable-dropdown/searchable-dropdown.module';
import { DotNavigationService } from '../../../dot-navigation/services/dot-navigation.service';
import { DotLoginAsComponent } from '../dot-login-as/dot-login-as.component';
import { DotMyAccountComponent } from '../dot-my-account/dot-my-account.component';

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
                DotToolbarUserStore
            ],
            imports: [
                BrowserAnimationsModule,
                DotDialogModule,
                DotIconModule,
                SearchableDropDownModule,
                RouterTestingModule,
                ButtonModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                FormsModule,
                ReactiveFormsModule,
                PasswordModule,
                CheckboxModule,
                HttpClientTestingModule,
                MenuModule,
                DotLoginAsComponent,
                DotMyAccountComponent,
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

        const logoutItem = de.query(By.css('#dot-toolbar-user-link-logout'));
        const logoutLink = logoutItem.query(By.css('a'));

        expect(logoutLink.attributes.href).toBe('/dotAdmin/logout?r=1466424490000');
        expect(logoutItem.classes['toolbar-user__logout']).toBe(true);
    });
    it('should have correct target in logout link', () => {
        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutLink = de.query(By.css('#dot-toolbar-user-link-logout a'));
        expect(logoutLink.attributes.target).toBe('_self');
    });

    it('should call "logoutAs" in "LoginService" on logout click', async () => {
        jest.spyOn(dotNavigationService, 'goToFirstPortlet').mockReturnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );
        jest.spyOn(locationService, 'reload');
        jest.spyOn(loginService, 'logoutAs');

        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutAsLink = de.query(By.css('#dot-toolbar-user-link-logout-as a')).nativeElement;
        logoutAsLink.click();

        await fixture.whenStable();
        expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    });

    it('should hide login as link', () => {
        jest.spyOn(loginService, 'getCurrentUser').mockReturnValue(
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

    it('should show mask', () => {
        fixture.detectChanges();
        const avatarComponent = de.query(By.css('[data-testid="avatar"]')).nativeElement;
        avatarComponent.click();

        fixture.detectChanges();
        const mask = de.query(By.css('[data-testId="dot-mask"]'));
        expect(mask).toBeTruthy();
    });

    it('should hide mask', () => {
        fixture.detectChanges();
        const avatarComponent = de.query(By.css('[data-testid="avatar"]')).nativeElement;
        avatarComponent.click();

        fixture.detectChanges();
        const mask = de.query(By.css('[data-testid="dot-mask"]'));
        mask.nativeElement.click();

        fixture.detectChanges();
        expect(de.query(By.css('[data-testid="dot-mask"]'))).toBeFalsy();
    });

    it('should hide mask when menu hide', () => {
        fixture.detectChanges();
        const avatarComponent = de.query(By.css('[data-testid="avatar"]')).nativeElement;
        avatarComponent.click();

        const menu = de.query(By.css('p-menu'));
        menu.triggerEventHandler('onHide', {});

        fixture.detectChanges();

        expect(de.query(By.css('[data-testId="dot-mask"]'))).toBeNull();
    });
});
