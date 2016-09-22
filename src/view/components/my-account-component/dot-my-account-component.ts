import {BaseComponent} from '../common/_base/base-component';
import {AccountService, AccountUser} from '../../../api/services/account-service';
import {Component, EventEmitter, Output, ViewEncapsulation} from '@angular/core';
import {LoginService, User, Auth} from '../../../api/services/login-service';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MessageService} from '../../../api/services/messages-service';
import {StringFormat} from '../../../api/util/stringFormat';

@Component({
    directives: [ MD_INPUT_DIRECTIVES ],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [AccountService, StringFormat],
    selector: 'dot-my-account',
    styleUrls: ['dot-my-account-component.css'],
    templateUrl: ['dot-my-account-component.html'],
})

export class MyAccountComponent extends BaseComponent{
    @Output() close  = new EventEmitter<>();

    private accountUser: AccountUser = {
        email: '',
        givenName: '',
        surname: '',
        userId: '',
    };
    private password: string;
    private passwordConfirm: string;
    private passwordMatch: boolean;
    private successMessage: string;
    private message = null;

    constructor(private loginService: LoginService, private accountService: AccountService,
                private messageService: MessageService, private stringFormat: StringFormat) {
        super(['modes.Close', 'save', 'error.form.mandatory', 'errors.email', 'First-Name',
            'Last-Name', 'email-address', 'password', 're-enter-password', 'error.forgot.password.passwords.dont.match',
            'message.createaccount.success', 'Error-communicating-with-server-Please-try-again'], messageService);
        this.passwordMatch = false;
        this.loginService.watchUser(this.loadUser.bind(this));
    }

    private getRequiredMessage(item) {
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
        this.password = '';
        this.passwordConfirm = '';
    }

    checkPasswords() {
        if (this.message) {
            this.message = null;
        }
        this.passwordMatch = this.password != '' && this.passwordConfirm != '' && this.password === this.passwordConfirm;
    }

    private save(): void {

        if (this.password) {
            this.accountUser.password = this.password;
        }

        this.accountService.updateUser(this.accountUser).subscribe(response => {
            // TODO: replace the alert with a Angular components
            alert(this.successMessage);
            this.close.emit();

            if (response.entity.reauthenticate) {
                this.loginService.logOutUser();
            } else {
                this.loginService.setAuth({
                    user: response.entity.user,
                    loginAsUser: null
                });
            }
        }, response => {
            // TODO: We have to define how must be the user feedback in case of error
            console.log(response.errorsMessages);
            this.message = response.errorsMessages;
        });
    }
}
