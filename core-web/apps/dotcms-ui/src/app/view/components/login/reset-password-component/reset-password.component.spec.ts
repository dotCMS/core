import { throwError } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { ResetPasswordComponent } from './reset-password.component';

import { MockDotLoginPageStateService } from '../dot-login-page-resolver.service.spec';
import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

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

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ResetPasswordComponent, BrowserAnimationsModule, RouterTestingModule],
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
        jest.spyOn(activatedRoute.snapshot.paramMap, 'get').mockReturnValue('test@test.com');

        fixture.detectChanges();
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
        const changePasswordButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));
        expect(changePasswordButton.nativeElement.disabled).toBe(true);
    });

    it('should display message if passwords do not match', () => {
        const changePasswordButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));
        jest.spyOn(loginService, 'changePassword');
        component.resetPasswordForm.setValue({
            password: 'test',
            confirmPassword: 'test2'
        });

        changePasswordButton.triggerEventHandler('click', {});
        fixture.detectChanges();
        const errorMessage = de.query(By.css('[data-testid="errorMessage"]'));

        expect(errorMessage.nativeElement.textContent).toBe('password do not match');
        expect(loginService.changePassword).not.toHaveBeenCalled();
    });

    it('should call the change password service and redirect to loging page', () => {
        const changePasswordButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));

        jest.spyOn(loginService, 'changePassword');
        component.resetPasswordForm.setValue({
            password: 'test',
            confirmPassword: 'test'
        });
        changePasswordButton.triggerEventHandler('click', {});
        expect(loginService.changePassword).toHaveBeenCalledWith('test', 'test@test.com');
        expect(loginService.changePassword).toHaveBeenCalledTimes(1);
        expect(dotRouterService.goToLogin).toHaveBeenCalledWith({
            queryParams: {
                changedPassword: true
            }
        });
    });

    it('should show error message form the service', () => {
        const changePasswordButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));

        jest.spyOn(loginService, 'changePassword').mockReturnValue(
            throwError({ error: { errors: [{ message: 'error message' }] } })
        );
        component.resetPasswordForm.setValue({
            password: 'test',
            confirmPassword: 'test'
        });
        changePasswordButton.triggerEventHandler('click', {});
        fixture.detectChanges();
        const errorMessage = de.query(By.css('[data-testId="errorMessage"]')).nativeElement
            .textContent;

        expect(errorMessage).toEqual('error message');
    });

    it('should show error message for required form fields', () => {
        const changePasswordButton: DebugElement = de.query(By.css('[data-testId="submitButton"]'));

        component.resetPasswordForm.setValue({
            password: 'abcd',
            confirmPassword: 'dcba'
        });

        changePasswordButton.triggerEventHandler('click', {});
        fixture.detectChanges();

        const errorMessage = de.query(By.css('[data-testId="errorMessage"]')).nativeElement
            .textContent;

        expect(errorMessage).toEqual('password do not match');
    });
});
