/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of, throwError } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { DotAccountService } from '@dotcms/app/api/services/dot-account-service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { StringFormat } from '@dotcms/app/api/util/stringFormat';
import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsConfigService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import {
    DotDialogModule,
    DotFieldRequiredDirective,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import {
    CoreWebServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService,
    mockUser
} from '@dotcms/utils-testing';

import { DotMyAccountComponent } from './dot-my-account.component';

class DotAccountServiceMock {
    addStarterPage() {
        return of({});
    }

    removeStarterPage() {
        return of({});
    }

    updateUser() {
        return of({});
    }
}

describe('DotMyAccountComponent', () => {
    let component: DotMyAccountComponent;
    let fixture: ComponentFixture<DotMyAccountComponent>;
    let de: DebugElement;
    let dotMenuService: DotMenuService;
    let dotcmsConfigService: DotcmsConfigService;
    let httpErrorManagerService: DotHttpErrorManagerService;
    let dotAlertConfirmService: DotAlertConfirmService;
    let dotAccountService: DotAccountService;
    let loginService: LoginService;
    let dotRouterService: DotRouterService;

    const messageServiceMock = new MockDotMessageService({
        'First-Name': 'First Name',
        'Last-Name': 'Last Name',
        'email-address': 'Email',
        Password: 'Password',
        'current-password': 'Current Password',
        'new-password': 'New Password',
        're-enter-new-password': 'Re-enter New Password',
        'error.form.mandatory': 'This field is required',
        'message.createaccount.success': 'Account created successfully',
        'change-password': 'Change Password',
        'reset-password': 'Reset Password',
        'error.forgot.password.passwords.dont.match': "Passwords don't match",
        save: 'Save',
        cancel: 'Cancel',
        'errors.email': 'Please enter a valid email address',
        'my-account': 'My account',
        'starter.show.getting.started': 'Show Getting Started'
    });

    beforeEach(waitForAsync(() => {
        dotMenuService = jasmine.createSpyObj('DotMenuService', ['isPortletInMenu']);
        dotcmsConfigService = jasmine.createSpyObj('DotcmsConfigService', ['getConfig']);
        httpErrorManagerService = jasmine.createSpyObj('DotHttpErrorManagerService', ['handle']);
        dotAlertConfirmService = jasmine.createSpyObj('DotAlertConfirmService', ['alert']);

        (dotMenuService.isPortletInMenu as jasmine.Spy).and.returnValue(of(false));
        (dotcmsConfigService.getConfig as jasmine.Spy).and.returnValue(of({ emailRegex: '' }));

        TestBed.configureTestingModule({
            imports: [
                DotMyAccountComponent,
                HttpClientTestingModule,
                CommonModule,
                FormsModule,
                ButtonModule,
                PasswordModule,
                InputTextModule,
                DialogModule,
                CheckboxModule,
                ProgressSpinnerModule,
                DotSafeHtmlPipe,
                DotFieldRequiredDirective,
                DotMessagePipe,
                DotDialogModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotHttpErrorManagerService, useValue: httpErrorManagerService },
                { provide: DotcmsConfigService, useValue: dotcmsConfigService },
                { provide: DotMenuService, useValue: dotMenuService },
                { provide: DotAccountService, useClass: DotAccountServiceMock },
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotAlertConfirmService, useValue: dotAlertConfirmService },
                ConfirmationService,
                LoggerService,
                StringUtils,
                StringFormat,
                UserModel
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotMyAccountComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotMenuService = TestBed.inject(DotMenuService);
        dotcmsConfigService = TestBed.inject(DotcmsConfigService);
        httpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        dotAccountService = TestBed.inject(DotAccountService);
        loginService = TestBed.inject(LoginService);
        dotRouterService = TestBed.inject(DotRouterService);

        // Spy on component's shutdown event
        spyOn(component.shutdown, 'emit');

        fixture.detectChanges(); // First detect changes to initialize the component
        component.visible = true; // Then set visible property
        fixture.detectChanges(); // Detect changes again after setting visible
    }));

    afterEach(() => {
        component.visible = false;
        fixture.detectChanges();
    });

    it(`should have right labels`, async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        component.form.setValue({
            givenName: 'Admin',
            surname: 'Admin',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: null,
            confirmPassword: null
        });

        fixture.detectChanges();
        const firstName = de.nativeElement.querySelector(
            '#dot-my-account-first-name-input'
        ).parentNode;
        const lasttName = de.nativeElement.querySelector(
            '#dot-my-account-last-name-input'
        ).parentNode;
        const email = de.nativeElement.querySelector('#dot-my-account-email-input').parentNode;
        const currentPassword = de.nativeElement.querySelector(
            '#dot-my-account-current-password-input'
        ).parentNode;
        const changePassword = de.nativeElement.querySelector(
            '#dot-my-account-change-password-option'
        );
        const newPassword = de.nativeElement.querySelector(
            '#dot-my-account-new-password-input'
        ).parentNode;
        const confirmPassword = de.nativeElement.querySelector(
            '#dot-my-account-confirm-new-password-input'
        ).parentNode;
        const showStarter = de.query(By.css('[data-testid="showStarterBtn"]'));
        const cancel = de.nativeElement.querySelector('.dialog__button-cancel');
        const save = de.nativeElement.querySelector('.dialog__button-accept');

        expect(firstName.innerText).toContain(messageServiceMock.get('First-Name'));
        expect(lasttName.innerText).toContain(messageServiceMock.get('Last-Name'));
        expect(email.innerText).toContain(messageServiceMock.get('email-address'));
        expect(currentPassword.innerText).toContain(messageServiceMock.get('current-password'));
        expect(changePassword.innerText).toContain(messageServiceMock.get('change-password'));
        expect(newPassword.innerText).toContain(messageServiceMock.get('new-password'));
        expect(confirmPassword.innerText).toContain(
            messageServiceMock.get('re-enter-new-password')
        );
        expect(showStarter.nativeElement.innerText).toContain(
            messageServiceMock.get('starter.show.getting.started')
        );
        expect(cancel.innerText).toEqual(messageServiceMock.get('modes.Close'));
        expect(save.innerText).toEqual(messageServiceMock.get('save'));
    });

    it(`should form be valid and load starter page data`, async () => {
        spyOn<any>(dotMenuService, 'isPortletInMenu').and.returnValue(of(true));
        fixture.detectChanges();
        await fixture.whenStable();

        component.form.setValue({
            givenName: 'Admin',
            surname: 'Admin',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: null,
            confirmPassword: null
        });
        fixture.detectChanges();
        const save = de.nativeElement.querySelector('.dialog__button-accept');

        expect(component.form.valid).toBe(true);
        expect(
            de.query(By.css('[data-testid="showStarterBtn"]')).attributes['ng-reflect-model']
        ).toBe('true');
        expect(save.disabled).toBe(false);
    });

    it(`should enable new passwords inputs when checked change password option`, async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        component.form.setValue({
            givenName: 'Admin',
            surname: 'Admin',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: '',
            confirmPassword: ''
        });
        fixture.detectChanges();
        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();

        await fixture.whenStable();

        fixture.detectChanges();
        const newPassword = de.nativeElement.querySelector('#dot-my-account-new-password-input');
        const confirmPassword = de.nativeElement.querySelector(
            '#dot-my-account-confirm-new-password-input'
        );
        expect(newPassword.disabled).toBe(false);
        expect(confirmPassword.disabled).toBe(false);
    });

    it(`should disabled SAVE when new passwords don't match`, async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        component.form.setValue({
            givenName: 'Admin2',
            surname: 'Admi2',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: 'newPassword',
            confirmPassword: 'newPasswrd'
        });
        fixture.detectChanges();
        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();

        await fixture.whenStable();

        fixture.detectChanges();
        expect(component.form.valid).toBe(false);
    });

    it(`should call to add starter method in account service`, async () => {
        spyOn<any>(dotMenuService, 'isPortletInMenu').and.returnValue(of(false));
        spyOn<any>(dotAccountService, 'addStarterPage').and.returnValue(of({ entity: {} }));
        spyOn<any>(dotAccountService, 'updateUser').and.returnValue(
            of({ entity: { user: mockUser() } })
        );

        fixture.detectChanges();
        await fixture.whenStable();

        const user = {
            givenName: 'Admin2',
            surname: 'Admi2',
            email: 'admin@dotcms.com',
            password: 'admin',
            newPassword: 'newPassword',
            confirmPassword: 'newPassword'
        };
        component.form.setValue(user);
        fixture.detectChanges();
        de.query(By.css('[data-testid="showStarterBtn"]')).componentInstance.onChange.emit(true);
        fixture.detectChanges();
        de.query(By.css('.dialog__button-accept')).triggerEventHandler('click', {});
        expect(dotAccountService.addStarterPage).toHaveBeenCalledTimes(1);
    });

    it(`should call to remove starter method in account service`, async () => {
        spyOn<any>(dotMenuService, 'isPortletInMenu').and.returnValue(of(true));
        spyOn<any>(dotAccountService, 'removeStarterPage').and.returnValue(of({ entity: {} }));
        spyOn<any>(dotAccountService, 'updateUser').and.returnValue(
            of({ entity: { user: mockUser() } })
        );
        fixture.detectChanges();
        await fixture.whenStable();
        const user = {
            givenName: 'Admin',
            surname: 'Admin',
            email: 'admin@dotcms.com',
            password: 'admin',
            newPassword: 'newPassword',
            confirmPassword: 'newPassword'
        };
        component.form.setValue(user);
        fixture.detectChanges();
        de.query(
            By.css('[data-testid="showStarterBtn"] input[type="checkbox"]')
        ).nativeElement.click();
        fixture.detectChanges();
        de.query(By.css('.dialog__button-accept')).triggerEventHandler('click', {});
        expect(dotAccountService.removeStarterPage).toHaveBeenCalledTimes(1);
    });

    it(`should SAVE form and sethAuth when no reauthentication`, async () => {
        spyOn<any>(dotAccountService, 'addStarterPage').and.returnValue(of({}));
        spyOn<any>(dotAccountService, 'removeStarterPage').and.returnValue(of({}));
        spyOn<any>(dotAccountService, 'updateUser').and.returnValue(
            of({ entity: { user: mockUser() } })
        );
        spyOn(loginService, 'setAuth');
        spyOn(component.shutdown, 'emit');

        fixture.detectChanges();
        await fixture.whenStable();

        const user = {
            givenName: 'Admin2',
            surname: 'Admi2',
            email: 'admin@dotcms.com',
            password: 'admin',
            newPassword: 'newPassword',
            confirmPassword: 'newPassword'
        };
        component.form.setValue(user);
        fixture.detectChanges();
        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();
        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();
        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(dotAccountService.updateUser).toHaveBeenCalledWith(component.dotAccountUser);
        expect(loginService.setAuth).toHaveBeenCalledWith({
            loginAsUser: null,
            user: mockUser()
        });
    });

    it(`should SAVE form and reauthenticate`, async () => {
        spyOn<any>(dotAccountService, 'addStarterPage').and.returnValue(of({}));
        spyOn<any>(dotAccountService, 'removeStarterPage').and.returnValue(of({}));
        spyOn<any>(dotAccountService, 'updateUser').and.returnValue(
            of({ entity: { reauthenticate: true } })
        );
        spyOn(dotAlertConfirmService, 'alert');
        spyOn(component.shutdown, 'emit');
        spyOn(dotRouterService, 'doLogOut');

        fixture.detectChanges();
        await fixture.whenStable();

        const user = {
            givenName: 'Admin2',
            surname: 'Admi2',
            email: 'admin@dotcms.com',
            password: 'admin',
            newPassword: 'newPassword',
            confirmPassword: 'newPassword'
        };
        component.form.setValue(user);
        fixture.detectChanges();
        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();
        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();
        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(dotAccountService.updateUser).toHaveBeenCalledWith(component.dotAccountUser);
        expect(dotAlertConfirmService.alert).toHaveBeenCalledWith({
            header: messageServiceMock.get('my-account'),
            message: messageServiceMock.get('message.createaccount.success')
        });
        expect(dotRouterService.doLogOut).toHaveBeenCalledTimes(1);
    });

    it(`should show password input error message when the new password not meet the system security requirements`, async () => {
        const errorResponse = {
            status: 400,
            error: {
                errors: [
                    {
                        errorCode: 'User-Info-Save-Password-Failed',
                        fieldName: null,
                        message: 'amazing message from backend'
                    }
                ]
            }
        };
        spyOn(dotAccountService, 'updateUser').and.returnValue(throwError(errorResponse));

        fixture.detectChanges();
        await fixture.whenStable();

        component.form.setValue({
            givenName: 'Admin2',
            surname: 'Admi2',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: 'admin',
            confirmPassword: 'admin'
        });

        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();

        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();

        const passwordFailMsg: DebugElement = de.query(
            By.css('[data-testId="dotSavePasswordFailedMsg"]')
        );

        expect(passwordFailMsg.nativeElement.innerText.trim()).toEqual(
            errorResponse.error.errors[0].message.trim()
        );
    });

    it(`should show current password input error message`, async () => {
        const errorResponse = {
            status: 400,
            error: {
                errors: [
                    {
                        errorCode: 'User-Info-Confirm-Current-Password-Failed',
                        fieldName: null,
                        message: 'amazing message from backend'
                    }
                ]
            }
        };
        spyOn(dotAccountService, 'updateUser').and.returnValue(throwError(errorResponse));

        fixture.detectChanges();
        await fixture.whenStable();

        component.form.setValue({
            givenName: 'Admin2',
            surname: 'Admi2',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: 'admin',
            confirmPassword: 'admin'
        });

        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();

        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();

        const passwordFailMsg: DebugElement = de.query(
            By.css('[data-testId="dotCurrrentPasswordFailedMsg"]')
        );

        expect(passwordFailMsg.nativeElement.innerText).toContain(
            errorResponse.error.errors[0].message.trim()
        );
    });

    it(`should call to HttpErrorManagerServices to show a generic dialog error message `, async () => {
        const errorResponse = {
            status: 400,
            error: {
                errors: [
                    {
                        errorCode: 'Any-Other-ErrorCode-From-Backend',
                        message: 'unknown message from backend'
                    }
                ]
            }
        };
        spyOn(dotAccountService, 'updateUser').and.returnValue(throwError(errorResponse));
        spyOn(httpErrorManagerService, 'handle').and.returnValue(of(null));

        fixture.detectChanges();
        await fixture.whenStable();

        component.form.setValue({
            givenName: 'abc',
            surname: 'abc',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: 'admin',
            confirmPassword: 'admin'
        });

        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('onChange', {});
        fixture.detectChanges();

        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();

        expect(httpErrorManagerService.handle).toHaveBeenCalledTimes(1);
    });

    it(`should show error message when form is invalid`, async () => {
        fixture.detectChanges();
        expect(component.form.valid).toBe(false);
    });
});
