/* eslint-disable @typescript-eslint/no-explicit-any */

import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { Injectable } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Params, RouterLink } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { delay } from 'rxjs/operators';

import { DotFormatDateService, DotMessageService, DotRouterService } from '@dotcms/data-access';
import { CoreWebService, LoggerService, LoginService, StringUtils } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    LoginServiceMock,
    mockLoginFormResponse,
    mockUser
} from '@dotcms/utils-testing';

import { DotLoginComponent } from './dot-login.component';

import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

const mockLoginInfo: DotLoginInformation = {
    ...mockLoginFormResponse,
    i18nMessagesMap: {
        ...mockLoginFormResponse.i18nMessagesMap,
        emailAddressLabel: 'Email Address',
        'angular.login.component.community.licence.message':
            ' - <a href="https://dotcms.com/features" target="_blank">upgrade</a>'
    }
};

const loginInfoSubject = new BehaviorSubject<DotLoginInformation>(mockLoginInfo);
const queryParamsSubject = new BehaviorSubject<Params>({});

@Injectable()
class MockDotLoginPageStateService {
    update = jest.fn();
    set = jest.fn().mockReturnValue(of(mockLoginInfo));
    get = () => loginInfoSubject.asObservable();
}

class ActivatedRouteMock {
    queryParams = queryParamsSubject;
}

describe('DotLoginComponent', () => {
    let spectator: Spectator<DotLoginComponent>;
    let component: DotLoginComponent;
    let loginService: LoginService;
    let dotRouterService: DotRouterService;
    let loginPageStateService: MockDotLoginPageStateService;
    let dotMessageService: DotMessageService;
    let dotFormatDateService: DotFormatDateService;

    const credentials = {
        login: 'admin@dotcms.com',
        language: 'en_US',
        password: 'admin',
        rememberMe: false,
        backEndLogin: true
    };

    const createComponent = createComponentFactory({
        component: DotLoginComponent,
        imports: [RouterTestingModule, NoopAnimationsModule, RouterLink],
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
        ],
        detectChanges: false
    });

    beforeEach(() => {
        loginInfoSubject.next(mockLoginInfo);
        queryParamsSubject.next({});
        spectator = createComponent();
        component = spectator.component;
        loginService = spectator.inject(LoginService);
        dotRouterService = spectator.inject(DotRouterService);
        loginPageStateService = spectator.inject(
            DotLoginPageStateService
        ) as unknown as MockDotLoginPageStateService;
        dotMessageService = spectator.inject(DotMessageService);
        dotFormatDateService = spectator.inject(DotFormatDateService);
        jest.spyOn(dotMessageService, 'init');
        spectator.detectChanges();
    });

    describe('Functionality', () => {
        it('should load form labels correctly', () => {
            const header = spectator.query(byTestId('header'));
            const emailLabel = spectator.query(byTestId('emailLabel'));
            const passwordLabel = spectator.query(byTestId('passwordLabel'));
            const recoverPasswordLink = spectator.query(byTestId('actionLink'));
            const checkboxContainer = spectator.query('.checkbox');
            const submitButton = spectator.query(byTestId('submitButton'));
            const serverInformation = spectator.query(byTestId('server'));
            const versionInformation = spectator.query(byTestId('version'));
            const licenseInformation = spectator.query(byTestId('license'));

            expect(header?.innerHTML.trim()).toContain('Welcome!');
            expect(emailLabel?.innerHTML.trim()).toEqual('Email Address');
            expect(passwordLabel?.innerHTML.trim()).toEqual('Password');
            expect(recoverPasswordLink?.innerHTML.trim()).toEqual('Recover Password');
            expect(checkboxContainer?.textContent?.trim()).toContain('Remember Me');
            expect(submitButton?.innerHTML.trim()).toContain('Sign In');
            expect(serverInformation?.innerHTML.trim()).toEqual('Server: 860173b0');
            expect(versionInformation?.innerHTML.trim()).toEqual(
                'COMMUNITY EDITION: 5.0.0 - March 13, 2019'
            );
            expect(licenseInformation?.innerHTML).toEqual(
                ' - <a href="https://dotcms.com/features" target="_blank">upgrade</a>'
            );
        });

        it('should call services on language change', () => {
            component.onLanguageChange('es_ES');

            expect(dotMessageService.init).toHaveBeenCalledWith({ language: 'es_ES' });
            expect(dotMessageService.init).toHaveBeenCalledTimes(1);
            expect(loginPageStateService.update).toHaveBeenCalledWith('es_ES');
            expect(loginPageStateService.update).toHaveBeenCalledTimes(1);
        });

        it('should have a link to forgot password', () => {
            const forgotPasswordLink = spectator.query(byTestId('actionLink'));
            expect(forgotPasswordLink?.getAttribute('href')).toEqual('/public/forgotPassword');
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
            jest.spyOn(dotFormatDateService, 'setLang');
            jest.spyOn(dotRouterService, 'goToMain');
            jest.spyOn(loginService as any, 'loginUser').mockReturnValue(
                of({
                    ...mockUser(),
                    editModeUrl: 'redirect/to'
                })
            );
            spectator.detectChanges();

            const signInButton = spectator.query(byTestId('submitButton'));
            expect(signInButton?.hasAttribute('disabled')).toBeFalsy();
            spectator.click(byTestId('submitButton'));
            expect(loginService.loginUser).toHaveBeenCalledWith(credentials);
            expect(loginService.loginUser).toHaveBeenCalledTimes(1);
            expect(dotRouterService.goToMain).toHaveBeenCalledWith('redirect/to');
            expect(dotRouterService.goToMain).toHaveBeenCalledTimes(1);
            expect(dotFormatDateService.setLang).toHaveBeenCalledWith('en_US');
            expect(dotFormatDateService.setLang).toHaveBeenCalledTimes(1);
        });

        it('should set loading while waiting login response', fakeAsync(() => {
            component.loginForm.setValue(credentials);
            jest.spyOn(dotRouterService, 'goToMain').mockResolvedValue(true);
            jest.spyOn(loginService as any, 'loginUser').mockReturnValue(
                of({
                    ...mockUser(),
                    editModeUrl: 'redirect/to'
                }).pipe(delay(200))
            );
            component.logInUser();
            tick(0);
            expect(component.loading()).toBe(true);
            tick(200);
            expect(component.loading()).toBe(false);
        }));

        it('should keep submit button disabled until the form is valid', () => {
            const signInButton = spectator.query(byTestId('submitButton'));
            expect(signInButton?.hasAttribute('disabled')).toBeTruthy();
        });

        it('should show error message for required form fields', () => {
            const loginControl = component.loginForm.get('login');
            loginControl?.setValue('');
            loginControl?.markAsTouched();
            loginControl?.markAsDirty();
            loginControl?.updateValueAndValidity();

            const passwordControl = component.loginForm.get('password');
            passwordControl?.setValue('');
            passwordControl?.markAsTouched();
            passwordControl?.markAsDirty();
            passwordControl?.updateValueAndValidity();

            spectator.detectChanges();

            const errorsMessages = spectator.queryAll('.p-invalid');
            expect(errorsMessages.length).toBe(2);
        });

        it('should show error messages if error comes from the server', () => {
            component.loginForm.setValue(credentials);
            jest.spyOn(loginService as any, 'loginUser').mockReturnValue(
                throwError({
                    status: 400,
                    error: { errors: [{ message: 'error message' }] }
                })
            );
            component.logInUser();
            spectator.detectChanges();
            const message = spectator.query(byTestId('message'));
            expect(message).toHaveClass('p-invalid');
            expect(message?.textContent).toEqual('error message');
        });
    });

    describe('Success messages', () => {
        it('should show password changed', () => {
            queryParamsSubject.next({ changedPassword: 'test' });
            loginInfoSubject.next(mockLoginInfo);
            spectator.detectChanges();
            expect(component.message).toEqual('Your password has been successfully changed');
            expect(component.isError).toBe(false);
        });

        it('should show email reset notification', () => {
            queryParamsSubject.next({
                resetEmailSent: 'true',
                resetEmail: 'test@email.com'
            });
            loginInfoSubject.next(mockLoginInfo);
            spectator.detectChanges();
            expect(component.message).toEqual(
                'An Email with instructions has been sent to test@email.com.'
            );
            expect(component.isError).toBe(false);
        });
    });
});
