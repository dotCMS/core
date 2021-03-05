import { ForgotPasswordComponent } from '@components/login/forgot-password-component/forgot-password.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';
import { By } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { of, throwError } from 'rxjs';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotLoginPageStateService } from '@components/login/dot-login-page-resolver.service.spec';
import { MockDotRouterService } from '@tests/dot-router-service.mock';

import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

const messageServiceMock = new MockDotMessageService({
    required: 'Required'
});

describe('ForgotPasswordComponent', () => {
    let component: ForgotPasswordComponent;
    let fixture: ComponentFixture<ForgotPasswordComponent>;
    let de: DebugElement;
    let dotRouterService: DotRouterService;
    let loginService: LoginService;
    let requestPasswordButton;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [ForgotPasswordComponent],
            imports: [
                BrowserAnimationsModule,
                FormsModule,
                ReactiveFormsModule,
                ButtonModule,
                InputTextModule,
                DotFieldValidationMessageModule,
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
        requestPasswordButton = de.query(By.css('[data-testid="submitButton"]'));
    });

    it('should load form labels correctly', () => {
        const header: DebugElement = de.query(By.css('[data-testId="header"]'));
        const inputLabel: DebugElement = de.query(By.css('[data-testId="usernameLabel"]'));
        const cancelButton: DebugElement = de.query(By.css('[data-testId="cancelButton"]'));
        const submitButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));

        expect(header.nativeElement.innerHTML).toEqual('Forgot Password');
        expect(inputLabel.nativeElement.innerHTML).toEqual('Email Address');
        expect(cancelButton.nativeElement.innerHTML).toContain('Cancel');
        expect(submitButton.nativeElement.innerHTML).toContain('Recover Password');
    });

    it('should keep recover password button disabled until the form is valid', () => {
        expect(requestPasswordButton.nativeElement.disabled).toBe(true);
    });

    it('should do the request password correctly and redirect to login', () => {
        component.forgotPasswordForm.setValue({ login: 'test' });
        spyOn(loginService, 'recoverPassword').and.returnValue(of({}));
        spyOn(window, 'confirm').and.returnValue(true);
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
        component.forgotPasswordForm.get('login').markAsDirty();
        fixture.detectChanges();

        const errorMessages = de.queryAll(By.css('dot-field-validation-message .p-invalid'));
        expect(errorMessages.length).toBe(1);
    });

    it('should show error message', () => {
        spyOn(window, 'confirm').and.returnValue(true);
        spyOn(loginService, 'recoverPassword').and.returnValue(
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
        spyOn(window, 'confirm').and.returnValue(true);
        spyOn(loginService, 'recoverPassword').and.returnValue(of({}));
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
        const cancelButton = de.query(By.css('.p-button-secondary'));
        cancelButton.triggerEventHandler('click', {});

        expect(dotRouterService.goToLogin).toHaveBeenCalledWith(undefined);
    });
});
