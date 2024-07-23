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
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

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
import { DotDialogModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
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
    addStarterPage() {}

    removeStarterPage() {}

    updateUser() {}
}

describe('DotMyAccountComponent', () => {
    let fixture: ComponentFixture<DotMyAccountComponent>;
    let comp: DotMyAccountComponent;
    let de: DebugElement;
    let dotAccountService: DotAccountService;
    let loginService: LoginService;
    let dotRouterService: DotRouterService;
    let dotMenuService: DotMenuService;
    let dotAlertConfirmService: DotAlertConfirmService;
    let httpErrorManagerService: DotHttpErrorManagerService;

    const messageServiceMock = new MockDotMessageService({
        'my-account': 'My Account',
        'modes.Close': 'Close',
        save: 'Save',
        'error.form.mandatory': 'This is mandatory',
        'errors.email': 'Wrong email',
        'First-Name': 'First Name',
        'Last-Name': 'Last Name',
        'email-address': 'Email',
        'new-password': 'New password',
        're-enter-new-password': 'Confirm password',
        'error.forgot.password.passwords.dont.match': 'Passwords do no match',
        'message.createaccount.success': 'Success',
        'Error-communicating-with-server-Please-try-again': 'Server error, try again!',
        'change-password': 'Change Password',
        'current-password': 'Current Password',
        'starter.show.getting.started': 'Show starter'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotMyAccountComponent],
            imports: [
                PasswordModule,
                InputTextModule,
                FormsModule,
                DotDialogModule,
                CommonModule,
                CheckboxModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                HttpClientTestingModule
            ],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotAccountService, useClass: DotAccountServiceMock },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                StringFormat,
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotcmsConfigService,
                LoggerService,
                StringUtils,
                DotMenuService,
                UserModel,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService
            ]
        });

        fixture = TestBed.createComponent(DotMyAccountComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotAccountService = TestBed.inject(DotAccountService);
        loginService = TestBed.inject(LoginService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        dotMenuService = TestBed.inject(DotMenuService);
        httpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);

        comp.visible = true;
    }));

    afterEach(() => {
        comp.visible = false;
        fixture.detectChanges();
    });

    it(`should have right labels`, async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        comp.form.setValue({
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

        comp.form.setValue({
            givenName: 'Admin',
            surname: 'Admin',
            email: 'admin@dotcms.com',
            password: 'test',
            newPassword: null,
            confirmPassword: null
        });
        fixture.detectChanges();
        const save = de.nativeElement.querySelector('.dialog__button-accept');

        expect(comp.form.valid).toBe(true);
        expect(
            de.query(By.css('[data-testid="showStarterBtn"]')).attributes['ng-reflect-model']
        ).toBe('true');
        expect(save.disabled).toBe(false);
    });

    it(`should enable new passwords inputs when checked change password option`, async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        comp.form.setValue({
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
        comp.form.setValue({
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
        expect(comp.dialogActions.accept.disabled).toBe(true);
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
            givenName: 'Admin',
            surname: 'Admin',
            email: 'admin@dotcms.com',
            password: 'admin',
            newPassword: 'newPassword',
            confirmPassword: 'newPassword'
        };
        comp.form.setValue(user);
        fixture.detectChanges();
        de.query(
            By.css('[data-testid="showStarterBtn"] input[type="checkbox"]')
        ).nativeElement.click();
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
        comp.form.setValue(user);
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
        spyOn(comp.shutdown, 'emit');

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
        comp.form.setValue(user);
        fixture.detectChanges();
        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('click', {});
        fixture.detectChanges();

        await fixture.whenStable();

        fixture.detectChanges();
        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();
        expect(comp.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(dotAccountService.updateUser).toHaveBeenCalledWith(comp.dotAccountUser);
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
        spyOn(comp.shutdown, 'emit');

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
        comp.form.setValue(user);
        fixture.detectChanges();
        const changePassword = de.query(By.css('#dot-my-account-change-password-option'));
        changePassword.triggerEventHandler('click', {});
        fixture.detectChanges();
        await fixture.whenStable();

        fixture.detectChanges();
        const save = de.query(By.css('.dialog__button-accept'));
        save.triggerEventHandler('click', {});
        fixture.detectChanges();
        expect(comp.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(dotAccountService.updateUser).toHaveBeenCalledWith(comp.dotAccountUser);
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

        comp.form.setValue({
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

        comp.form.setValue({
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

        comp.form.setValue({
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
});
