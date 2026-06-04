import { throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import {
    DotcmsEventsServiceMock,
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
            throwError(() => ({ error: { errors: [{ message: 'error message' }] } }))
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

describe('ResetPasswordComponent — HTTP contract', () => {
    // Uses the real LoginService (not mocked) so the outgoing HTTP request
    // is exercised. Regression guard for #35269: Angular HttpClient auto-sets
    // Content-Type based on the body's runtime type — an object gets
    // application/json, a string gets text/plain. The backend only accepts
    // application/json, so the body MUST be an object.
    let fixture: ComponentFixture<ResetPasswordComponent>;
    let component: ResetPasswordComponent;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ResetPasswordComponent, BrowserAnimationsModule, RouterTestingModule],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
                { provide: DotLoginPageStateService, useClass: MockDotLoginPageStateService },
                { provide: DotRouterService, useClass: MockDotRouterService }
            ]
        });

        fixture = TestBed.createComponent(ResetPasswordComponent);
        component = fixture.componentInstance;
        httpMock = TestBed.inject(HttpTestingController);
        jest.spyOn(TestBed.inject(ActivatedRoute).snapshot.paramMap, 'get').mockReturnValue(
            'reset-token'
        );
        fixture.detectChanges();
    });

    afterEach(() => httpMock.verify());

    it('should POST /api/v1/changePassword with a plain-object body (not a stringified JSON)', () => {
        component.resetPasswordForm.setValue({ password: 'new-pass', confirmPassword: 'new-pass' });
        fixture.detectChanges();
        fixture.debugElement
            .query(By.css('[data-testId="submitButton"]'))
            .triggerEventHandler('click', {});

        const req = httpMock.expectOne('/api/v1/changePassword');

        expect(req.request.method).toBe('POST');
        // If the body were a string, Angular would set Content-Type: text/plain and
        // the backend (@Consumes APPLICATION_JSON) would respond 415.
        expect(typeof req.request.body).toBe('object');
        expect(req.request.body).toEqual({ password: 'new-pass', token: 'reset-token' });

        req.flush({ entity: 'ok' });
    });

    it('should not crash when the error body has no "errors" array', () => {
        component.resetPasswordForm.setValue({ password: 'new-pass', confirmPassword: 'new-pass' });
        fixture.detectChanges();
        fixture.debugElement
            .query(By.css('[data-testId="submitButton"]'))
            .triggerEventHandler('click', {});

        const req = httpMock.expectOne('/api/v1/changePassword');
        // Simulate the 415 body shape observed in production (no `errors` array).
        req.flush(
            { message: 'HTTP 415 Unsupported Media Type' },
            { status: 415, statusText: 'Unsupported Media Type' }
        );
        fixture.detectChanges();

        // Component must survive without throwing "Cannot read properties of undefined (reading '0')"
        // AND should surface the backend's fallback `message` so the user isn't left with a blank state.
        expect(component.message).toBe('HTTP 415 Unsupported Media Type');
    });
});
