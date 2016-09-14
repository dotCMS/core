
import {Component, EventEmitter, Input, Output, ViewEncapsulation, ViewChild, ElementRef} from '@angular/core';
import {Control, ValidationResult, Validators} from '@angular/common';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {LoginService, User} from '../../../api/services/login-service';
import {AccountService, AccountUser} from '../../../api/services/account-service';
import { Router } from '@ngrx/router';
import {Observable} from 'rxjs/Rx';

@Component({
    directives: [ MD_INPUT_DIRECTIVES ],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [AccountService],
    selector: 'dot-my-account',
    styleUrls: ['dot-my-account-component.css'],
    templateUrl: ['dot-my-account-component.html'],
})
export class MyAccountComponent {

    @Output() close  = new EventEmitter<>();

    private accountUser: AccountUser = {
        email: '',
        givenName: '',
        surname: '',
        userId: '',
    };

    private message: string;
    private password: string;
    private confirmPassword: string;
    private confirmPasswordIsValid: boolean = true;

    // TODO: change when new FE internationalization is done
    private i18nMessages: Array<string> = [ 'modes.Close', 'save', 'error.form.mandatory', 'errors.email', 'First-Name',
        'Last-Name', 'email-address', 'password', 're-enter-password', 'error.forgot.password.passwords.dont.match',
        'message.createaccount.success', 'Error-communicating-with-server-Please-try-again'];

    private saveButtonLabel: string;
    private closeButtonLabel: string;
    private firstNameLabel: string;
    private lastNameLabel: string;
    private emailLabel: string;
    private passwordLabel: string;
    private confirmPasswordLabel: string;

    private firsNameErrorMessage: string;
    private lastNameErrorMessage: string;
    private mandatoryEmailErrorMessage: string;
    private passwordErrorMessage: string;
    private confirmPasswordErrorMessage: string;
    private invalidEmailErrorFormat: string;
    private invalidEmailErrorMessage: string;
    private successMessage: string;
    private errorCommunicatingWithServer: string;

    constructor(private loginService: LoginService, private accountService: AccountService, private router: Router) {}

    save(): void {

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
                this.loginService.setLogInUser(response.entity.user);
            }
        }, response => {
            // TODO: We have to define how must be the user feedback in case of error
            console.log(response.errorsMessages);
            this.message = response.errorsMessages;
        });
    }

    ngOnInit(): void {
        // TODO: change when new FE internationalization is done
        this.loginService.getLoginFormInfo('', this.i18nMessages).subscribe((data) => {

            // Translate labels and messages
            let dataI18n = data.i18nMessagesMap;

            this.saveButtonLabel = dataI18n['save'];
            this.closeButtonLabel = dataI18n['modes.Close'];

            let mandatoryFieldError: string = dataI18n['error.form.mandatory'];
            this.firstNameLabel = dataI18n['First-Name'];
            this.lastNameLabel = dataI18n['Last-Name'];
            this.emailLabel = dataI18n['email-address'];
            this.passwordLabel = dataI18n['password'];
            this.confirmPasswordLabel = dataI18n['re-enter-password'];

            this.firsNameErrorMessage = (mandatoryFieldError).replace('{0}', this.firstNameLabel);
            this.lastNameErrorMessage = (mandatoryFieldError).replace('{0}', this.lastNameLabel);
            this.mandatoryEmailErrorMessage = (mandatoryFieldError).replace('{0}', this.emailLabel);
            this.invalidEmailErrorFormat = dataI18n['errors.email'];
            this.passwordErrorMessage = (mandatoryFieldError).replace('{0}', this.passwordLabel);
            this.confirmPasswordErrorMessage =  dataI18n['error.forgot.password.passwords.dont.match'];
            this.successMessage = dataI18n['message.createaccount.success'];
            this.errorCommunicatingWithServer = dataI18n['Error-communicating-with-server-Please-try-again'];
        });

        if (this.loginService.loginUser) {
           this.loadUser(this.loginService.loginUser);
        }

        this.loginService.loginUser$.subscribe( user => {
            this.loadUser(user);
        });
    }

    changingEmail(): void {
        this.invalidEmailErrorMessage =  this.invalidEmailErrorFormat.replace('{0}', this.accountUser.email);
    }

    validateConfirmPassword(): void {
        this.confirmPasswordIsValid = this.password === this.confirmPassword;
    }

    private loadUser(user: User): void {
        this.accountUser.email = user.emailAddress;
        this.accountUser.givenName = user.firstName;
        this.accountUser.surname = user.lastName;
        this.password = '';
        this.confirmPassword = '';
        this.accountUser.userId = user.userId;
    }
}
