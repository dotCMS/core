/* eslint-disable @typescript-eslint/no-explicit-any */

import { BehaviorSubject, of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Params, RouterLink } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { Dropdown, DropdownModule } from 'primeng/dropdown';

import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotLoginComponent } from '@components/login/dot-login-component/dot-login.component';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotMessageService, DotRouterService, DotFormatDateService } from '@dotcms/data-access';
import { CoreWebService, LoggerService, LoginService, StringUtils } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';
import { DotFieldValidationMessageComponent } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    LoginServiceMock,
    mockLoginFormResponse,
    mockUser
} from '@dotcms/utils-testing';

const mockLoginInfo = {
    ...mockLoginFormResponse,
    i18nMessagesMap: {
        ...mockLoginFormResponse.i18nMessagesMap,
        emailAddressLabel: 'Email Address'
    }
};
const subject = new BehaviorSubject<DotLoginInformation>(mockLoginInfo);
const queryParams = new BehaviorSubject<Params>({});

@Injectable()
class MockDotLoginPageStateService {
    update = jasmine.createSpy('update');
    set = jasmine.createSpy('set').and.returnValue(of(mockLoginInfo));
    get = () => subject;
}

class ActivatedRouteMock {
    queryParams = queryParams;
}

describe('DotLoginComponent', () => {
    let component: DotLoginComponent;
    let fixture: ComponentFixture<DotLoginComponent>;
    let de: DebugElement;
    let loginService: LoginService;
    let dotRouterService: DotRouterService;
    let loginPageStateService: DotLoginPageStateService;
    let dotMessageService: DotMessageService;
    let dotFormatDateService: DotFormatDateService;
    let signInButton: DebugElement;
    const credentials = {
        login: 'admin@dotcms.com',
        language: 'en_US',
        password: 'admin',
        rememberMe: false,
        backEndLogin: true
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotLoginComponent],
            imports: [
                BrowserAnimationsModule,
                FormsModule,
                ButtonModule,
                CheckboxModule,
                DropdownModule,
                DotLoadingIndicatorModule,
                DotFieldValidationMessageComponent,
                RouterTestingModule,
                FormsModule,
                ReactiveFormsModule,
                HttpClientTestingModule,
                RouterLink
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotLoginPageStateService, useClass: MockDotLoginPageStateService },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: ActivatedRoute, useClass: ActivatedRouteMock },
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                DotMessageService,
                DotLoadingIndicatorService,
                DotRouterService,
                LoggerService,
                StringUtils
            ]
        });

        fixture = TestBed.createComponent(DotLoginComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        dotRouterService = de.injector.get(DotRouterService);
        dotFormatDateService = de.injector.get(DotFormatDateService);
        dotMessageService = de.injector.get(DotMessageService);
        loginPageStateService = de.injector.get(DotLoginPageStateService);
        spyOn(dotMessageService, 'init');
    });

    describe('Functionality', () => {
        beforeEach(() => {
            fixture.detectChanges();
            signInButton = de.query(By.css('[data-testId="submitButton"]'));
        });

        it('should load form labels correctly', () => {
            const header: DebugElement = de.query(By.css('[data-testId="header"]'));
            const emailLabel: DebugElement = de.query(By.css('[data-testId="emailLabel"]'));
            const passwordLabel: DebugElement = de.query(By.css('[data-testId="passwordLabel"]'));
            const recoverPasswordLink: DebugElement = de.query(
                By.css('[data-testId="actionLink"]')
            );
            const rememberMe: DebugElement = de.query(By.css('p-checkbox label'));
            const submitButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));
            const serverInformation: DebugElement = de.query(By.css('[data-testId="server"]'));
            const versionInformation: DebugElement = de.query(By.css('[data-testId="version"]'));
            const licenseInformation: DebugElement = de.query(By.css('[data-testId="license"]'));

            expect(header.nativeElement.innerHTML.trim()).toContain('Welcome!');
            expect(emailLabel.nativeElement.innerHTML.trim()).toEqual('Email Address');
            expect(passwordLabel.nativeElement.innerHTML.trim()).toEqual('Password');
            expect(recoverPasswordLink.nativeElement.innerHTML.trim()).toEqual('Recover Password');
            expect(rememberMe.nativeElement.innerHTML.trim()).toEqual('Remember Me');
            expect(submitButton.nativeElement.innerHTML.trim()).toContain('Sign In');
            expect(serverInformation.nativeElement.innerHTML.trim()).toEqual('Server: 860173b0');
            expect(versionInformation.nativeElement.innerHTML.trim()).toEqual(
                'COMMUNITY EDITION: 5.0.0 - March 13, 2019'
            );
            expect(licenseInformation.nativeElement.innerHTML).toEqual(
                ' - <a href="https://dotcms.com/features" target="_blank">upgrade</a>'
            );
        });

        it('should call services on language change', () => {
            const pDropDown: DebugElement = de.query(By.css('[data-testId="language"]'));
            pDropDown.triggerEventHandler('onChange', { value: 'es_ES' });

            expect(dotMessageService.init).toHaveBeenCalledWith({ language: 'es_ES' });
            expect(loginPageStateService.update).toHaveBeenCalledWith('es_ES');
        });

        it('should have a link to forgot password', () => {
            const forgotPasswordLink: DebugElement = de.query(By.css('[data-testId="actionLink"]'));
            expect(forgotPasswordLink.nativeElement.getAttribute('href')).toEqual(
                '/public/forgotPassword'
            );
        });

        it('should load initial value of the form', () => {
            expect(component.loginForm.value).toEqual({
                backEndLogin: true,
                language: 'en_US',
                login: '',
                password: '',
                rememberMe: false
            });
        });

        it('should make a login request correctly and redirect after login', () => {
            component.loginForm.setValue(credentials);
            spyOn(dotFormatDateService, 'setLang');
            spyOn(dotRouterService, 'goToMain');
            spyOn<any>(loginService, 'loginUser').and.returnValue(
                of({
                    ...mockUser(),
                    editModeUrl: 'redirect/to'
                })
            );
            fixture.detectChanges();

            expect(signInButton.nativeElement.disabled).toBeFalsy();
            signInButton.triggerEventHandler('click', {});
            expect(loginService.loginUser).toHaveBeenCalledWith(credentials);
            expect(dotRouterService.goToMain).toHaveBeenCalledWith('redirect/to');
            expect(dotFormatDateService.setLang).toHaveBeenCalledWith('en_US');
        });

        it('should disable fields while waiting login response', async () => {
            component.loginForm.setValue(credentials);
            spyOn(dotRouterService, 'goToMain');
            spyOn<any>(loginService, 'loginUser').and.returnValue(
                of({
                    ...mockUser(),
                    editModeUrl: 'redirect/to'
                })
            );
            signInButton.triggerEventHandler('click', {});

            fixture.detectChanges();
            await fixture.whenStable();

            const languageDropdown: Dropdown = de.query(
                By.css('[data-testId="language"]')
            ).componentInstance;
            const emailInput = de.query(By.css('[data-testId="userNameInput"]'));
            const passwordInput = de.query(By.css('[data-testId="password"]'));
            const rememberCheckBox = component.loginForm.get('rememberMe');

            expect(languageDropdown.disabled).toBeTruthy();
            expect(emailInput.nativeElement.disabled).toBeTruthy();
            expect(passwordInput.nativeElement.disabled).toBeTruthy();
            expect(rememberCheckBox.disable).toBeTruthy();
        });

        it('should keep submit button disabled until the form is valid', () => {
            expect(signInButton.nativeElement.disabled).toBeTruthy();
        });

        it('should show error message for required form fields', () => {
            const loginControl = component.loginForm.get('login');
            loginControl.setValue('');
            loginControl.markAsTouched();
            loginControl.markAsDirty();
            loginControl.updateValueAndValidity();

            const passwordControl = component.loginForm.get('password');
            passwordControl.setValue('');
            passwordControl.markAsTouched();
            passwordControl.markAsDirty();
            passwordControl.updateValueAndValidity();

            fixture.detectChanges();

            const errorsMessages = de.queryAll(By.css('.p-invalid'));
            expect(errorsMessages.length).toBe(2);
        });

        it('should show error messages if error comes from the server', () => {
            component.loginForm.setValue(credentials);
            spyOn(loginService, 'loginUser').and.returnValue(
                throwError({ status: 400, error: { errors: [{ message: 'error message' }] } })
            );
            signInButton.triggerEventHandler('click', {});
            fixture.detectChanges();
            const message: HTMLParagraphElement = de.query(
                By.css('[data-testId="message"]')
            ).nativeElement;
            expect(message).toHaveClass('p-invalid');
            expect(message.innerText).toEqual('error message');
        });
    });

    describe('Success messages', () => {
        it('should show password changed', () => {
            queryParams.next({ changedPassword: 'test' });
            fixture.detectChanges();
            const message: HTMLParagraphElement = de.query(
                By.css('[data-testId="message"]')
            ).nativeElement;
            expect(message).toHaveClass('success');
            expect(message.innerText).toEqual('Your password has been successfully changed');
        });

        it('should show email reset notification', () => {
            queryParams.next({ resetEmailSent: 'true', resetEmail: 'test@email.com' });
            fixture.detectChanges();
            const message: HTMLParagraphElement = de.query(
                By.css('[data-testId="message"]')
            ).nativeElement;
            expect(message).toHaveClass('success');
            expect(message.innerText).toEqual(
                'An Email with instructions has been sent to test@email.com.'
            );
        });
    });
});
