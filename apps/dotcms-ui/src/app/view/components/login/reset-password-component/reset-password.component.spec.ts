import { ResetPasswordComponent } from '@components/login/reset-password-component/reset-password.component';
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
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotLoginPageStateService } from '@components/login/dot-login-page-resolver.service.spec';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { throwError } from 'rxjs';

const messageServiceMock = new MockDotMessageService({
    required: 'Required'
});

describe('ResetPasswordComponent', () => {
    let component: ResetPasswordComponent;
    let fixture: ComponentFixture<ResetPasswordComponent>;
    let de: DebugElement;
    let loginService: LoginService;
    let activatedRoute: ActivatedRoute;
    let dotRouterService: DotRouterService;
    let changePasswordButton;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [ResetPasswordComponent],
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

        fixture = TestBed.createComponent(ResetPasswordComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        activatedRoute = TestBed.inject(ActivatedRoute);
        loginService = TestBed.inject(LoginService);
        dotRouterService = TestBed.inject(DotRouterService);
        spyOn(activatedRoute.snapshot.paramMap, 'get').and.returnValue('test@test.com');
        fixture.detectChanges();

        changePasswordButton = de.query(By.css('button[type="submit"]'));
    });

    it('should load form labels correctly', () => {
        const header: DebugElement = de.query(By.css('[data-testId="header"]'));
        const enterLabel: DebugElement = de.query(By.css('[data-testId="enterLabel"]'));
        const confirmLabel: DebugElement = de.query(By.css('[data-testId="confirmLabel"]'));
        const button: DebugElement = de.query(By.css('[data-testId="submitButton"]'));

        expect(header.nativeElement.innerHTML).toEqual('Password Reset');
        expect(enterLabel.nativeElement.innerHTML).toContain('Enter Password');
        expect(confirmLabel.nativeElement.innerHTML).toContain('Confirm Password');
        expect(button.nativeElement.innerHTML).toContain('Change Password');
    });

    it('should keep change password button disabled until the form is valid', () => {
        expect(changePasswordButton.nativeElement.disabled).toBe(true);
    });

    it('should display message if passwords do not match', () => {
        spyOn(loginService, 'changePassword').and.callThrough();
        component.resetPasswordForm.setValue({
            password: 'test',
            confirmPassword: 'test2'
        });
        changePasswordButton.triggerEventHandler('click', {});
        fixture.detectChanges();
        const errorMessage = de.query(By.css('[data-testid="errorMessage"]'));

        expect(errorMessage.nativeElement.innerText).toBe('password do not match');
        expect(loginService.changePassword).not.toHaveBeenCalled();
    });

    it('should call the change password service and redirect to loging page', () => {
        spyOn(loginService, 'changePassword').and.callThrough();
        component.resetPasswordForm.setValue({
            password: 'test',
            confirmPassword: 'test'
        });
        changePasswordButton.triggerEventHandler('click', {});
        expect(loginService.changePassword).toHaveBeenCalledWith('test', 'test@test.com');
        expect(dotRouterService.goToLogin).toHaveBeenCalledWith({
            queryParams: {
                changedPassword: true
            }
        });
    });

    it('should show error message form the service', () => {
        spyOn(loginService, 'changePassword').and.returnValue(
            throwError({ error: { errors: [{ message: 'error message' }] } })
        );
        component.resetPasswordForm.setValue({
            password: 'test',
            confirmPassword: 'test'
        });
        changePasswordButton.triggerEventHandler('click', {});
        fixture.detectChanges();
        const errorMessage = de.query(By.css('[data-testId="errorMessage"]')).nativeElement
            .innerText;

        expect(errorMessage).toEqual('error message');
    });

    it('should show error message for required form fields', () => {
        component.resetPasswordForm.get('password').markAsDirty();
        component.resetPasswordForm.get('confirmPassword').markAsDirty();
        fixture.detectChanges();

        const errorMessages = de.queryAll(By.css('dot-field-validation-message .p-invalid'));
        expect(errorMessages.length).toBe(2);
    });
});
