import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { PasswordModule, InputTextModule, CheckboxModule } from 'primeng/primeng';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMyAccountComponent } from './dot-my-account.component';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock, mockUser } from '@tests/login-service.mock';
import { AccountService } from '@services/account-service';
import { StringFormat } from 'src/app/api/util/stringFormat';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

describe('DotMyAccountComponent', () => {
    let fixture: ComponentFixture<DotMyAccountComponent>;
    let comp: DotMyAccountComponent;
    let de: DebugElement;
    let accountService: AccountService;
    let loginService: LoginService;

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
        'current-password': 'Current Password'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotMyAccountComponent],
            imports: [
                PasswordModule,
                MdInputTextModule,
                InputTextModule,
                FormsModule,
                DotDialogModule,
                CommonModule,
                CheckboxModule
            ],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                { provide: DotMessageService, useValue: messageServiceMock },
                AccountService,
                StringFormat
            ]
        });

        fixture = DOTTestBed.createComponent(DotMyAccountComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        accountService = fixture.debugElement.injector.get(AccountService);
        loginService = fixture.debugElement.injector.get(LoginService);

        comp.visible = true;
        fixture.detectChanges();
    }));

    it(`should have right labels`, () => {
        fixture.whenStable().then(() => {
            comp.form.setValue({
                givenName: 'Admin',
                surname: 'Admin',
                email: 'admin@dotcms.com',
                password: 'test',
                newPassword: null,
                confirmPassword: null
            });

            fixture.detectChanges();
            const firstName = de.nativeElement.querySelector('#dot-my-account-first-name-input')
                .parentNode;
            const lasttName = de.nativeElement.querySelector('#dot-my-account-last-name-input')
                .parentNode;
            const email = de.nativeElement.querySelector('#dot-my-account-email-input').parentNode;
            const currentPassword = de.nativeElement.querySelector(
                '#dot-my-account-current-password-input'
            ).parentNode;
            const changePassword = de.nativeElement.querySelector(
                '#dot-my-account-change-password-option'
            );
            const newPassword = de.nativeElement.querySelector('#dot-my-account-new-password-input')
                .parentNode;
            const confirmPassword = de.nativeElement.querySelector(
                '#dot-my-account-confirm-new-password-input'
            ).parentNode;
            const cancel = de.nativeElement.querySelector('.dialog__button-cancel');
            const save = de.nativeElement.querySelector('.dialog__button-accept');

            expect(firstName.innerText).toBe(messageServiceMock.get('First-Name'));
            expect(lasttName.innerText).toBe(messageServiceMock.get('Last-Name'));
            expect(email.innerText).toBe(messageServiceMock.get('email-address'));
            expect(currentPassword.innerText).toBe(messageServiceMock.get('current-password'));
            expect(changePassword.innerText).toBe(messageServiceMock.get('change-password'));
            expect(newPassword.innerText).toBe(messageServiceMock.get('new-password'));
            expect(confirmPassword.innerText).toBe(messageServiceMock.get('re-enter-new-password'));
            expect(cancel.innerText).toBe(messageServiceMock.get('modes.Close').toUpperCase());
            expect(save.innerText).toBe(messageServiceMock.get('save').toUpperCase());
        });
    });

    it(`should form be valid`, () => {
        fixture.whenStable().then(() => {
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
            expect(save.disabled).toBe(false);
        });
    });

    it(`should enable new passwords inputs when checked change password option`, () => {
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

        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const newPassword = de.nativeElement.querySelector(
                '#dot-my-account-new-password-input'
            );
            const confirmPassword = de.nativeElement.querySelector(
                '#dot-my-account-confirm-new-password-input'
            );
            expect(newPassword.disabled).toBe(false);
            expect(confirmPassword.disabled).toBe(false);
        });
    });

    it(`should disabled SAVE when new passwords don't match`, () => {
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

        fixture.whenStable().then(() => {
            fixture.detectChanges();
            expect(comp.dialogActions.accept.disabled).toBe(true);
        });
    });

    it(`should SAVE form and sethAuth when no reauthentication`, () => {
        spyOn(accountService, 'updateUser').and.returnValue(of({ entity: { user: mockUser } }));
        spyOn(loginService, 'setAuth');
        spyOn(comp.close, 'emit');

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

        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const save = de.query(By.css('.dialog__button-accept'));
            save.triggerEventHandler('click', {});
            fixture.detectChanges();
            expect(comp.close.emit).toHaveBeenCalledTimes(1);
            expect(accountService.updateUser).toHaveBeenCalledWith(comp.accountUser);
            expect(loginService.setAuth).toHaveBeenCalledWith({
                loginAsUser: null,
                user: mockUser
            });
        });
    });

    it(`should SAVE form and reauthenticate`, () => {
        spyOn(accountService, 'updateUser').and.returnValue(
            of({ entity: { reauthenticate: true } })
        );
        spyOn(loginService, 'logOutUser').and.returnValue(of({}));
        spyOn(comp.close, 'emit');

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

        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const save = de.query(By.css('.dialog__button-accept'));
            save.triggerEventHandler('click', {});
            fixture.detectChanges();
            expect(comp.close.emit).toHaveBeenCalledTimes(1);
            expect(accountService.updateUser).toHaveBeenCalledWith(comp.accountUser);
            expect(loginService.logOutUser).toHaveBeenCalledTimes(1);
        });
    });
});
