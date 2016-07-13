import {Component, Input, Inject, ViewEncapsulation} from '@angular/core';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton} from '@angular2-material/button';
import {MdToolbar} from '@angular2-material/toolbar';
import {MdCheckbox} from '@angular2-material/checkbox/checkbox';
import {MD_CARD_DIRECTIVES} from '@angular2-material/card';
import {LoginService} from '../../../../api/services/login-service';
import {assetUrl} from '@angular/compiler/src/util';

@Component({
    directives:[MdToolbar, MD_INPUT_DIRECTIVES, MdButton, MdCheckbox, MD_CARD_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [LoginService],
    selector: 'dot-login-component',
    styleUrls: ['login-component.css'],
    templateUrl: ['login-component.html']
})

/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginComponent {
    @Input() my_account_login: string= '@dotcms.com';
    @Input() password: string;
    @Input() my_account_r_m: boolean= false;
    @Input() forgotPasswordEmail: string;

    languages: Array<string> = [];
    @Input() language: string= 'en_US';
    message: string= '';

    // labels
    loginLabel: string= '';
    userIdOrEmailLabel: string= '';
    passwordLabel: string= '';
    rememberMeLabel: string= '';
    forgotPasswordLabel: string= '';
    loginButton: string= '';
    forgotPasswordButton: string= '';
    forgotPasswordConfirmationMessage: string= '';
    cancelButton: string= '';
    serverLabel: string = '';

    _loginService: LoginService;
    dotcmscompanyLogo: string= '';
    dotcmsServerId: string= '';
    dotcmslicenceLevel: string= '';
    dotcmsVersion: string= '';
    dotcmsBuildDateString: string= '';

    private i18nMessages: Array<string> = [ 'Login', 'email-address', 'user-id', 'password', 'remember-me', 'sign-in', 'forgot-password', 'get-new-password', 'cancel', 'an-email-with-instructions-will-be-sent','Server'];

    constructor(@Inject('menuItems') private menuItems: any[], private _service: LoginService) {
        this._loginService = _service;
        this.updateScreenBackground();
    }

    /**
     *  Executes the logIn service
     */
    logInUser(): void {

        this._loginService.logInUser(this.my_account_login, this.password, this.my_account_r_m, this.language).subscribe((result: any) => {
            if (result.errors.length > 0) {
                this.message = result.errors[0].message;
            } else {
                this.message = '';
                window.location.reload();
            }
        }, (error) => {
            if (error.response.status === 400 || error.response.status === 401) {
                this.message = this.getErrorMessage(error);
            } else {
                console.log(error);
                // this.message = getErrorMessage(error);
            }
        });

    }

    /**
     * Executes the recover password service
     */
    recoverPassword(): void {
        if (confirm(this.forgotPasswordConfirmationMessage)) {
            document.getElementById('forgotPassword').className = 'forgotPasswordBox hideBox';
            document.getElementById('loginBox').style.display = '';

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

        this._loginService.getLoginFormInfo( this.language, this.i18nMessages).subscribe((data) => {

            // Translate labels and messages
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;

            this.loginLabel = dataI18n['Login'];
            if ('emailAddress' === entity.authorizationType) {
                this.userIdOrEmailLabel = dataI18n['email-address'];
            } else {
                this.userIdOrEmailLabel = dataI18n['user-id'];
                this.my_account_login = '';
            }
            this.passwordLabel = dataI18n['password'];
            this.rememberMeLabel = dataI18n['remember-me'];
            this.loginButton = dataI18n['sign-in'];
            this.forgotPasswordLabel = dataI18n['forgot-password'];
            this.forgotPasswordButton = dataI18n['get-new-password'];
            this.cancelButton = dataI18n['cancel'];
            this.forgotPasswordConfirmationMessage = dataI18n['an-email-with-instructions-will-be-sent'];
            this.serverLabel = dataI18n['Server'];



            // Set background colors and images
            document.body.style.backgroundRepeat = 'no-repeat';
            document.body.style.backgroundPosition = 'top center';

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
                    this.languages.push(lang.language + '_' + lang.country);
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
        document.getElementById('forgotPassword').className = 'forgotPasswordBox hideBox';
        document.getElementById('loginBox').style.display = '';
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        document.getElementById('forgotPassword').className = 'forgotPasswordBox';
        document.getElementById('loginBox').style.display = 'none';
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
