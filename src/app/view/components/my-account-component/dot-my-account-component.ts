import {BaseComponent} from '../common/_base/base-component';
import {AccountService, AccountUser} from '../../../api/services/account-service';
import {Component, EventEmitter, Output, ViewEncapsulation, Input} from '@angular/core';
import {LoginService, User, Auth} from '../../../api/services/login-service';
import {MessageService} from '../../../api/services/messages-service';
import {StringFormat} from '../../../api/util/stringFormat';

@Component({
    encapsulation: ViewEncapsulation.None,

    selector: 'dot-my-account',
    styleUrls: ['dot-my-account-component.css'],
    templateUrl: 'dot-my-account-component.html',
})

export class MyAccountComponent extends BaseComponent {
    @Output() close  = new EventEmitter<>();
    @Input() visible: boolean;

    private accountUser: AccountUser = {
        currentPassword: '',
        email: '',
        givenName: '',
        surname: '',
        userId: ''
    };

    private passwordConfirm: string;
    private passwordMatch: boolean;
    private message = null;
    private changePasswordOption: boolean = false;

    constructor(private loginService: LoginService, private accountService: AccountService,
                private messageService: MessageService, private stringFormat: StringFormat) {
        super(['my-account', 'modes.Close', 'save', 'error.form.mandatory', 'errors.email', 'First-Name',
            'Last-Name', 'email-address', 'new-password', 're-enter-new-password', 'error.forgot.password.passwords.dont.match',
            'message.createaccount.success', 'Error-communicating-with-server-Please-try-again', 'change-password', 'current-password'], messageService);
        this.passwordMatch = false;
        this.changePasswordOption = false;
        this.loginService.watchUser(this.loadUser.bind(this));
    }

    checkPasswords(): void {
        if (this.message) {
            this.message = null;
        }
        this.passwordMatch = this.accountUser.newPassword !== '' && this.passwordConfirm !== '' &&
            this.accountUser.newPassword === this.passwordConfirm;
    }

    toggleChangePasswordOption(): void {
        this.changePasswordOption = !this.changePasswordOption;
    }

    private getRequiredMessage(item): void {
        return this.stringFormat.formatMessage(this.i18nMessages['error.form.mandatory'], item);
    }

    private loadUser(auth: Auth): void {
        let user: User = auth.user;
        this.accountUser = {
            email: user.emailAddress,
            givenName: user.firstName,
            surname: user.lastName,
            userId: user.userId
        };

        this.accountUser.newPassword = null;
        this.passwordConfirm = null;
    }

    private save(): void {
        this.accountService.updateUser(this.accountUser).subscribe(response => {
            // TODO: replace the alert with a Angular components
            alert(this.i18nMessages['message.createaccount.success']);
            this.close.emit();

            if (response.entity.reauthenticate) {
                this.loginService.logOutUser().subscribe(() => {});
            } else {
                this.loginService.setAuth({
                    loginAsUser: null,
                    user: response.entity.user
                });
            }
        }, response => {
            // TODO: We have to define how must be the user feedback in case of error
            this.message = response.errorsMessages;
        });
    }
}
