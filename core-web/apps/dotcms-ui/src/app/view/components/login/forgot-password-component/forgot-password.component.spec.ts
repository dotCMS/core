import { of, throwError } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotFieldValidationMessageComponent } from '@dotcms/ui';
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
    let component: ForgotPasswordComponent;
    let fixture: ComponentFixture<ForgotPasswordComponent>;
    let de: DebugElement;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [ForgotPasswordComponent],
            imports: [
                BrowserAnimationsModule,
                FormsModule,
                ReactiveFormsModule,
                ButtonModule,
                InputTextModule,
                DotFieldValidationMessageComponent,
                RouterTestingModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotLoginPageStateService, useClass: MockDotLoginPageStateService },
                { provide: DotRouterService, useClass: MockDotRouterService }
            ]
        });

        fixture = TestBed.createComponent(ForgotPasswordComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        loginService = de.injector.get(LoginService);
        dotRouterService = de.injector.get(DotRouterService);

        fixture.detectChanges();
    });

    it('should load form labels correctly', () => {
        const header: DebugElement = de.query(By.css('[data-testId="header"]'));
        const inputLabel: DebugElement = de.query(By.css('[data-testId="usernameLabel"]'));
        const cancelButton: DebugElement = de.query(By.css('[data-testId="cancelButton"]'));
        const submitButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));

        expect(header.nativeElement.innerHTML).toEqual('Forgot Password');
        expect(inputLabel.nativeElement.innerHTML).toContain('Email Address');
        expect(cancelButton.nativeElement.innerHTML).toContain('Cancel');
        expect(submitButton.nativeElement.innerHTML).toContain('Recover Password');
    });

    it('should keep recover password button disabled until the form is valid', () => {
        const requestPasswordButton = de.query(By.css('[data-testid="submitButton"]'));
        expect(requestPasswordButton.nativeElement.disabled).toBe(true);
    });

    it('should do the request password correctly and redirect to login', () => {
        const control = component.forgotPasswordForm.get('login');
        control.setValue('test');
        control.markAsTouched();
        control.markAsDirty();
        fixture.detectChanges();

        const requestPasswordButton = de.query(By.css('[data-testid="submitButton"]'));

        jest.spyOn(loginService, 'recoverPassword').mockReturnValue(of(null));
        jest.spyOn(window, 'confirm').mockReturnValue(true);
        fixture.detectChanges();

        expect(requestPasswordButton.nativeElement.disabled).toBeFalsy();
        requestPasswordButton.triggerEventHandler('click', {});

        expect(loginService.recoverPassword).toHaveBeenCalledWith('test');
        expect(dotRouterService.goToLogin).toHaveBeenCalledWith({
            queryParams: {
                resetEmailSent: true,
                resetEmail: 'test'
            }
        });
    });

    it('should show error message for required form fields', () => {
        const control = component.forgotPasswordForm.get('login');
        control.setValue('');
        control.markAsTouched();
        control.markAsDirty();
        control.updateValueAndValidity();

        fixture.detectChanges();

        const errorMessages = fixture.debugElement.queryAll(
            By.css('dot-field-validation-message .p-invalid')
        );

        expect(errorMessages.length).toBe(1);
    });

    it('should show error message', () => {
        const requestPasswordButton = de.query(By.css('[data-testid="submitButton"]'));
        jest.spyOn(window, 'confirm').mockReturnValue(true);
        jest.spyOn(loginService, 'recoverPassword').mockReturnValue(
            throwError({ error: { errors: [{ message: 'error message' }] } })
        );
        const input: HTMLInputElement = de.query(By.css('[data-testid="input"]')).nativeElement;
        input.value = 'test';

        requestPasswordButton.triggerEventHandler('click', {});
        fixture.detectChanges();
        const errorMessage = de.query(By.css('[data-testId="errorMessage"]')).nativeElement
            .innerText;
        expect(errorMessage).toEqual('error message');
    });

    it('should show go to login if submit is success', () => {
        const requestPasswordButton = de.query(By.css('[data-testid="submitButton"]'));

        jest.spyOn(window, 'confirm').mockReturnValue(true);
        jest.spyOn(loginService, 'recoverPassword').mockReturnValue(of(null));
        component.forgotPasswordForm.setValue({ login: 'test@test.com' });
        fixture.detectChanges();
        requestPasswordButton.triggerEventHandler('click', {});

        expect(dotRouterService.goToLogin).toHaveBeenCalledWith({
            queryParams: {
                resetEmailSent: true,
                resetEmail: 'test@test.com'
            }
        });
    });

    it('should call goToLogin when cancel button is clicked', () => {
        const cancelButton = de.query(By.css('[data-testid="cancelButton"]'));
        cancelButton.triggerEventHandler('click', {});

        expect(dotRouterService.goToLogin).toHaveBeenCalledWith(undefined);
    });
});
