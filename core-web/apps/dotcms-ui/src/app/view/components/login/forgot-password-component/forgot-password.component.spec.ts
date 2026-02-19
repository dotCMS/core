import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { ForgotPasswordComponent } from './forgot-password.component';

import { MockDotLoginPageStateService } from '../dot-login-page-resolver.service.spec';
import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

const messageServiceMock = new MockDotMessageService({
    required: 'Required'
});

describe('ForgotPasswordComponent', () => {
    let spectator: Spectator<ForgotPasswordComponent>;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;

    const createComponent = createComponentFactory({
        component: ForgotPasswordComponent,
        imports: [BrowserAnimationsModule, RouterTestingModule],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: DotLoginPageStateService, useClass: MockDotLoginPageStateService },
            { provide: DotRouterService, useClass: MockDotRouterService }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        loginService = spectator.inject(LoginService);
        dotRouterService = spectator.inject(DotRouterService);
    });

    it('should load form labels correctly', (done) => {
        const loginPageState = spectator.inject(
            DotLoginPageStateService
        ) as unknown as MockDotLoginPageStateService;
        loginPageState.get().subscribe((loginInfo) => {
            expect(loginInfo.i18nMessagesMap['forgot-password']).toEqual('Forgot Password');
            expect(loginInfo.i18nMessagesMap['emailAddressLabel']).toContain('Email Address');
            expect(loginInfo.i18nMessagesMap['cancel']).toContain('Cancel');
            expect(loginInfo.i18nMessagesMap['get-new-password']).toContain('Recover Password');
            done();
        });
    });

    it('should keep recover password button disabled until the form is valid', fakeAsync(() => {
        tick();
        spectator.detectChanges();
        expect(spectator.component.forgotPasswordForm.valid).toBe(false);
        const requestPasswordButton = spectator.debugElement.query(
            By.css('[data-testid="submitButton"]')
        );
        expect(requestPasswordButton?.nativeElement?.disabled ?? true).toBe(true);
    }));

    it('should do the request password correctly and redirect to login', fakeAsync(() => {
        tick();
        spectator.detectChanges();
        const control = spectator.component.forgotPasswordForm.get('login');
        control.setValue('test');
        control.markAsTouched();
        control.markAsDirty();
        spectator.detectChanges();

        jest.spyOn(loginService, 'recoverPassword').mockReturnValue(of(null));
        jest.spyOn(window, 'confirm').mockReturnValue(true);
        spectator.detectChanges();

        const requestPasswordButton = spectator.debugElement.query(
            By.css('[data-testid="submitButton"]')
        );
        expect(requestPasswordButton?.nativeElement.disabled).toBeFalsy();
        spectator.click('[data-testid="submitButton"]');

        expect(loginService.recoverPassword).toHaveBeenCalledWith('test');
        expect(loginService.recoverPassword).toHaveBeenCalledTimes(1);
        expect(dotRouterService.goToLogin).toHaveBeenCalledWith({
            queryParams: {
                resetEmailSent: true,
                resetEmail: 'test'
            }
        });
    }));

    it('should show error message for required form fields', fakeAsync(() => {
        tick();
        spectator.detectChanges();
        const control = spectator.component.forgotPasswordForm.get('login');
        control.setValue('');
        control.markAsTouched();
        control.markAsDirty();
        control.updateValueAndValidity();

        spectator.detectChanges();

        const errorMessages = spectator.debugElement.queryAll(
            By.css('dot-field-validation-message .p-invalid')
        );

        expect(errorMessages.length).toBe(1);
    }));

    it('should show error message', fakeAsync(() => {
        tick();
        spectator.detectChanges();
        jest.spyOn(window, 'confirm').mockReturnValue(true);
        jest.spyOn(loginService, 'recoverPassword').mockReturnValue(
            throwError({ error: { errors: [{ message: 'error message' }] } })
        );
        spectator.component.forgotPasswordForm.setValue({ login: 'test' });
        spectator.detectChanges();
        spectator.click('[data-testid="submitButton"]');
        tick();
        spectator.detectChanges();
        expect(spectator.component.message).toEqual('error message');
        const errorMessageEl = spectator.debugElement.query(By.css('[data-testid="errorMessage"]'));
        if (errorMessageEl?.nativeElement?.textContent) {
            expect(errorMessageEl.nativeElement.textContent.trim()).toEqual('error message');
        }
    }));

    it('should show go to login if submit is success', fakeAsync(() => {
        tick();
        spectator.detectChanges();

        jest.spyOn(window, 'confirm').mockReturnValue(true);
        jest.spyOn(loginService, 'recoverPassword').mockReturnValue(of(null));
        spectator.component.forgotPasswordForm.setValue({ login: 'test@test.com' });
        spectator.detectChanges();
        spectator.click('[data-testid="submitButton"]');
        tick();

        expect(dotRouterService.goToLogin).toHaveBeenCalledWith({
            queryParams: {
                resetEmailSent: true,
                resetEmail: 'test@test.com'
            }
        });
    }));

    it('should call goToLogin when cancel button is clicked', fakeAsync(() => {
        tick();
        spectator.detectChanges();
        spectator.click('[data-testid="cancelButton"]');

        expect(dotRouterService.goToLogin).toHaveBeenCalledWith(undefined);
        expect(dotRouterService.goToLogin).toHaveBeenCalledTimes(1);
    }));
});
