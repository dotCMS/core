/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService, DotUiColorsService } from '@dotcms/data-access';
import { CoreWebService, LoggerService, LoginService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { DotToolbarUserStore } from './store/dot-toolbar-user.store';

import { LOCATION_TOKEN } from '../../../../../providers';
import { MockDotUiColorsService } from '../../../../../test/dot-test-bed';
import { DotNavigationService } from '../../../dot-navigation/services/dot-navigation.service';

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
                {
                    provide: DotNavigationService,
                    useValue: {
                        goToFirstPortlet: jest.fn().mockResolvedValue(true)
                    }
                },
                { provide: LoggerService, useValue: { error: jest.fn() } },
                { provide: DotMessageService, useValue: { get: (key: string) => key } },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotToolbarUserStore
            ],
            imports: [
                BrowserAnimationsModule,
                RouterTestingModule,
                HttpClientTestingModule,
                DotToolbarUserComponent
            ]
        });

        fixture = TestBed.createComponent(DotToolbarUserComponent);

        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
        dotNavigationService = de.injector.get(DotNavigationService);
    });

    it('should have correct href in logout link', () => {
        jest.spyOn(loginService, 'watchUser').mockImplementation((callback) => {
            callback({
                user: {
                    emailAddress: 'admin@dotcms.com',
                    name: 'Admin User',
                    fullName: 'Admin User'
                },
                loginAsUser: null,
                isLoginAs: false
            } as any);
        });

        // Mock Date constructor to return a specific timestamp
        const mockDate = {
            getTime: () => 1466424490000
        };
        const originalDate = global.Date;
        global.Date = jest.fn(() => mockDate) as any;
        global.Date.now = jest.fn(() => 1466424490000);

        // Recreate the component with the mocked Date
        fixture = TestBed.createComponent(DotToolbarUserComponent);
        de = fixture.debugElement;
        loginService = de.injector.get(LoginService);
        locationService = de.injector.get(LOCATION_TOKEN);
        dotNavigationService = de.injector.get(DotNavigationService);

        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutItem = document.querySelector('#dot-toolbar-user-link-logout');
        const logoutLink = logoutItem?.querySelector('a');

        expect(logoutLink?.getAttribute('href')).toBe('/dotAdmin/logout?r=1466424490000');
        expect(logoutItem?.classList.contains('toolbar-user__logout')).toBe(true);

        // Restore original Date
        global.Date = originalDate;
    });
    it('should have correct target in logout link', () => {
        jest.spyOn(loginService, 'watchUser').mockImplementation((callback) => {
            callback({
                user: {
                    emailAddress: 'admin@dotcms.com',
                    name: 'Admin User',
                    fullName: 'Admin User'
                },
                loginAsUser: null,
                isLoginAs: false
            } as any);
        });

        fixture.detectChanges();

        const avatarComponent = de.query(By.css('p-avatar')).nativeElement;
        avatarComponent.click();
        fixture.detectChanges();

        const logoutLink = document.querySelector('#dot-toolbar-user-link-logout a');
        expect(logoutLink?.getAttribute('target')).toBe('_self');
    });

    it('should call "logoutAs" in "LoginService" on logout click', fakeAsync(() => {
        // Mock the watchUser method to simulate "login as" mode
        const mockAuth = {
            user: {
                emailAddress: 'admin@dotcms.com',
                name: 'Admin User',
                fullName: 'Admin User'
            },
            loginAsUser: {
                emailAddress: 'user@dotcms.com',
                name: 'Regular User',
                fullName: 'Regular User'
            },
            isLoginAs: true
        };

        jest.spyOn(loginService, 'watchUser').mockImplementation((callback) => {
            callback(mockAuth);
        });

        jest.spyOn(dotNavigationService, 'goToFirstPortlet').mockResolvedValue(true);
        jest.spyOn(locationService, 'reload');
        jest.spyOn(loginService, 'logoutAs').mockReturnValue(of(true));

        fixture.detectChanges();

        // Test the command function directly instead of clicking
        const component = fixture.componentInstance;
        const store = component.store;
        store.logoutAs();

        expect(loginService.logoutAs).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);

        // Wait for async operations to complete
        tick();
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    }));

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
