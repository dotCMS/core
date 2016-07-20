import {Component, EventEmitter, Inject, Input, Output, ViewEncapsulation} from '@angular/core';
import {LoginService} from '../../../../api/services/login-service';

// angular material imports
import {MdButton} from '@angular2-material/button';
import {MD_CARD_DIRECTIVES} from '@angular2-material/card';
import {MdCheckbox} from '@angular2-material/checkbox/checkbox';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdToolbar} from '@angular2-material/toolbar';

@Component({
    directives: [MdButton, MD_CARD_DIRECTIVES, MdCheckbox, MD_INPUT_DIRECTIVES, MdToolbar],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [LoginService],
    selector: 'dot-login-component',
    styleUrls: ['login-component.css'],
    templateUrl: ['login-component.html'],
})

/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginComponent {
    @Input() myAccountLogin: string;
    @Input() password: string;
    @Input() myAccountRememberMe: boolean = false;
    @Input() forgotPasswordEmail: string;
    @Input() language: string = '';
    @Output() toggleMain = new EventEmitter<boolean>();

    languages: Array<string> = [];
    message: string = '';

    // labels
    loginLabel: string = '';
    emailAddressLabel: string = ''
    userIdOrEmailLabel: string = '';
    passwordLabel: string = '';
    rememberMeLabel: string = '';
    forgotPasswordLabel: string = '';
    loginButton: string = '';
    forgotPasswordButton: string = '';
    forgotPasswordConfirmationMessage: string = '';
    cancelButton: string = '';
    serverLabel: string = '';
    dotcmscompanyLogo: string = '';
    dotcmsServerId: string = '';
    dotcmslicenceLevel: string = '';
    dotcmsVersion: string = '';
    dotcmsBuildDateString: string = '';

    isForgotPasswordCardHidden: boolean = true;
    isLoginCardHidden: boolean = false;

    private i18nMessages: Array<string> = [ 'Login', 'email-address', 'user-id', 'password', 'remember-me', 'sign-in', 'forgot-password', 'get-new-password', 'cancel', 'an-email-with-instructions-will-be-sent','Server'];

    constructor(@Inject('menuItems') private menuItems: any[], private _loginService: LoginService) {
        this.updateScreenBackground();
    }

    /**
     *  Executes the logIn service
     */
    logInUser(): void {

        this._loginService.logInUser(this.myAccountLogin, this.password, this.myAccountRememberMe, this.language).subscribe((result: any) => {
            if (result.errors.length > 0) {
                this.message = result.errors[0].message;
            } else {
                this.message = '';
                this.toggleMain.emit(false);
                // this window.location.reload should be removed once the menu and router injection update issue is fixed
                window.location.reload();
            }
        }, (error) => {
            if (error.response.status === 400 || error.response.status === 401) {
                this.message = this.getErrorMessage(error);
            } else {
                console.log(error);
            }
        });

    }

    /**
     * Executes the recover password service
     */
    recoverPassword(): void {
        if (confirm(this.forgotPasswordConfirmationMessage)) {
            this.isForgotPasswordCardHidden = true;
            this.isLoginCardHidden = false;

            this._loginService.recoverPassword(this.forgotPasswordEmail).subscribe((result: any) => {

            }, (error) => {
                console.log(error);
                this.message = this.getErrorMessage(error);
            });
        }
    }

    /**
     * Execute the change language service
     */
    changeLanguage(lang: string): void {
        this.language = lang;
        this.updateScreenBackground();
    }

    /**
     * Update the color and or image according to the values specified
     */
    private updateScreenBackground(): void {

        this._loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe((data) => {

            // Translate labels and messages
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;

            this.loginLabel = dataI18n.Login;
            this.emailAddressLabel = dataI18n['email-address'];
            if ('emailAddress' === entity.authorizationType) {
                this.userIdOrEmailLabel = dataI18n['email-address'];
            } else {
                this.userIdOrEmailLabel = dataI18n['user-id'];
                this.myAccountLogin = '';
            }
            this.passwordLabel = dataI18n.password;
            this.rememberMeLabel = dataI18n['remember-me'];
            this.loginButton = dataI18n['sign-in'].toUpperCase();
            this.forgotPasswordLabel = dataI18n['forgot-password'];
            this.forgotPasswordButton = dataI18n['get-new-password'];
            this.cancelButton = dataI18n.cancel;
            this.forgotPasswordConfirmationMessage = dataI18n['an-email-with-instructions-will-be-sent'];
            this.serverLabel = dataI18n.Server;

            // Set background color and image with the values provided by the service
            if (entity.backgroundColor !== 'undefined' && entity.backgroundColor !== '') {
                document.body.style.backgroundColor = entity.backgroundColor;
            }
            if (entity.backgroundPicture !== 'undefined' && entity.backgroundPicture !== '') {
                document.body.style.backgroundImage = 'url(' + entity.backgroundPicture + ')';
            }

            // Set dotCMS Info
            this.dotcmscompanyLogo = entity.logo;
            this.dotcmsServerId = entity.serverId;
            this.dotcmslicenceLevel = entity.levelName;
            this.dotcmsVersion = entity.version;
            this.dotcmsBuildDateString = entity.buildDateString;

            // Configure languages
            if (this.languages.length === 0) {
                entity.languages.forEach(lang => {
                    this.languages.push({
                        label: lang.country,
                        value: lang.language + '_' + lang.country
                    });
                });
            }
        }, (error) => {
             console.log(error);
        });
    }

    /**
     * Display the login form instad of the forgot password form
     */
    cancelRecoverPassword(): void {
        this.isForgotPasswordCardHidden = true;
        this.isLoginCardHidden = false;
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        this.isForgotPasswordCardHidden = false;
        this.isLoginCardHidden = true;
    }

    /**
     * Get the error messages internationalized
     * @param error server response
     * @returns {string}
     */
    private getErrorMessage(error: any): string {
        let errorObject = JSON.parse(error.response._body);
        let errorMessages = '';
        try {
            errorObject.errors.forEach(e => {
                errorMessages += e.message;
            });
        }catch (ex) {
            errorMessages = errorObject.error.split(':')[1];
        }
        return errorMessages;
    }

}
