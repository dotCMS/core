import { DotLoginComponent } from '@components/login/dot-login-component/dot-login.component';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '@tests/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock, mockUser } from '@tests/login-service.mock';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import {
    Checkbox,
    CheckboxModule,
    Dropdown,
    DropdownModule,
    InputTextModule
} from 'primeng/primeng';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { MockDotLoginPageStateService } from '@components/login/dot-login-page-resolver.service.spec';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

describe('DotLoginComponent', () => {
    let component: DotLoginComponent;
    let fixture: ComponentFixture<DotLoginComponent>;
    let de: DebugElement;
    let loginService: LoginService;
    let dotRouterService: DotRouterService;
    let loginPageStateService: DotLoginPageStateService;
    let dotMessageService: DotMessageService;
    let signInButton: DebugElement;
    const credentials = {
        login: 'admin@dotcms.com',
        language: 'en_US',
        password: 'admin',
        rememberMe: false,
        backEndLogin: true
    };

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotLoginComponent],
            imports: [
                BrowserAnimationsModule,
                FormsModule,
                ButtonModule,
                CheckboxModule,
                DropdownModule,
                MdInputTextModule,
                InputTextModule,
                DotLoadingIndicatorModule,
                DotFieldValidationMessageModule,
                RouterTestingModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotLoginPageStateService, useClass: MockDotLoginPageStateService },
                DotMessageService,
                DotLoadingIndicatorService
            ]
        });

        fixture = DOTTestBed.createComponent(DotLoginComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        loginService = de.injector.get(LoginService);
        dotRouterService = de.injector.get(DotRouterService);
        loginPageStateService = de.injector.get(DotLoginPageStateService);
        dotMessageService = de.injector.get(DotMessageService);
        spyOn(dotMessageService, 'init');
        fixture.detectChanges();
        signInButton = de.query(By.css('button[pButton]'));
    });

    it('should load form labels correctly', () => {
        const header: DebugElement = de.query(By.css('h3'));
        const inputLabels: DebugElement[] = de.queryAll(By.css('span[dotmdinputtext] label'));
        const recoverPasswordLink: DebugElement = de.query(By.css('a[actionlink]'));
        const rememberMe: DebugElement = de.query(By.css('p-checkbox label'));
        const submitButton: DebugElement = de.query(By.css('.login__button'));
        const productInformation: DebugElement[] = de.queryAll(By.css('.login__footer span'));
        expect(header.nativeElement.innerHTML).toEqual('Welcome!');
        expect(inputLabels[0].nativeElement.innerHTML).toEqual('Email Address');
        expect(inputLabels[1].nativeElement.innerHTML).toEqual('Password');
        expect(recoverPasswordLink.nativeElement.innerHTML).toEqual('Recover Password');
        expect(rememberMe.nativeElement.innerHTML).toEqual('Remember Me');
        expect(submitButton.nativeElement.innerHTML).toContain('Sign In');
        expect(productInformation[0].nativeElement.innerHTML).toEqual('Server: 860173b0');
        expect(productInformation[1].nativeElement.innerHTML).toEqual(
            'COMMUNITY EDITION: 5.0.0 - March 13, 2019'
        );
        expect(productInformation[2].nativeElement.innerHTML).toEqual(
            ' - <a href="https://dotcms.com/features" target="_blank">upgrade</a>'
        );
    });

    it('should init messages on page load with default language', () => {
        expect(dotMessageService.init).toHaveBeenCalledWith(true);
    });

    it('should call services on language change', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
        pDropDown.triggerEventHandler('onChange', { value: 'es_ES' });

        expect(dotMessageService.init).toHaveBeenCalledWith(true, 'es_ES');
        expect(loginPageStateService.update).toHaveBeenCalledWith('es_ES');
    });

    it('should navigate to the recover password screen', () => {
        const forgotPasswordLink: DebugElement = de.query(By.css('a[actionLink]'));

        forgotPasswordLink.triggerEventHandler('click', { value: '' });

        expect(dotRouterService.goToForgotPassword).toHaveBeenCalledTimes(1);
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
        spyOn(loginService, 'loginUser').and.callThrough();
        spyOn(dotMessageService, 'setRelativeDateMessages');
        fixture.detectChanges();

        expect(signInButton.nativeElement.disabled).toBeFalsy();
        signInButton.triggerEventHandler('click', {});
        expect(loginService.loginUser).toHaveBeenCalledWith(credentials);
        expect(dotRouterService.goToMain).toHaveBeenCalledWith('redirect/to');
        expect(dotMessageService.setRelativeDateMessages).toHaveBeenCalledWith(mockUser.languageId);
    });

    it('should disable fields while waiting login response', () => {
        component.loginForm.setValue(credentials);
        spyOn(loginService, 'loginUser').and.callThrough();
        signInButton.triggerEventHandler('click', {});

        const languageDropdown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        const emailInput = de.query(By.css('input[pInputText][type="text"]'));
        const passwordInput = de.query(By.css('input[type="password"]'));
        const rememberCheckBox: Checkbox = de.query(By.css(' p-checkbox')).componentInstance;

        fixture.detectChanges();

        expect(languageDropdown.disabled).toBeTruthy();
        expect(emailInput.nativeElement.disabled).toBeTruthy();
        expect(passwordInput.nativeElement.disabled).toBeTruthy();
        expect(rememberCheckBox.disabled).toBeTruthy();
    });

    it('should keep submit button disabled until the form is valid', () => {
        expect(signInButton.nativeElement.disabled).toBeTruthy();
    });

    it('should show error message for required form fields', () => {
        component.loginForm.get('login').markAsDirty();
        component.loginForm.get('password').markAsDirty();

        fixture.detectChanges();

        const erroresMessages = de.queryAll(By.css('.ui-messages-error'));
        expect(erroresMessages.length).toBe(2);
    });

    it('should show messages', () => {
        component.message = 'Authentication failed. Please try again.';
        fixture.detectChanges();
        const messageElemement = de.query(By.css('.error-message'));
        expect(messageElemement).not.toBeNull();
    });
});
