import {Component, EventEmitter, Input, NgZone, Output, ViewEncapsulation} from '@angular/core';
import {LoginData} from './login-container';
import {LoginService} from '../../../../api/services/login-service';
import {LoggerService} from '../../../../api/services/logger.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-login-component',
    templateUrl: 'login-component.html',
})

/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginComponent {
    @Input() isLoginInProgress = false;
    @Input()  message = '';
    @Input() passwordChanged = false;
    @Input() resetEmailSent = false;
    @Input() resetEmail = '';

    @Output() recoverPassword  = new EventEmitter<any>();
    @Output() login  = new EventEmitter<LoginData>();

    private myAccountLogin: string;
    private password: string;
    private myAccountRememberMe = false;
    private language = '';

    private languages: Array<any> = [];

    // labels
    private cancelButton = '';
    private communityLicenseInfoMessage = '';
    private dotcmsBuildDateString = '';
    private dotcmscompanyLogo = '';
    private dotcmslicenceLevel = '';
    private dotcmsServerId = '';
    private dotcmsVersion = '';
    private emailAddressLabel = '';
    private forgotPasswordButton = '';
    private loginButton = '';
    private loginLabel = '';
    private mandatoryFieldError = '';
    private passwordLabel = '';
    private rememberMeLabel = '';
    private resetEmailMessage = '';
    private resetPasswordSuccess = '';
    private serverLabel = '';
    private userIdOrEmailLabel = '';

    private isCommunityLicense = true;

    private i18nMessages: Array<string> = [ 'Login', 'email-address', 'user-id', 'password', 'remember-me', 'sign-in',
       'get-new-password', 'cancel', 'Server', 'error.form.mandatory',
       'angular.login.component.community.licence.message', 'reset-password-success',
       'a-new-password-has-been-sent-to-x'];

    constructor(private loginService: LoginService, private ngZone: NgZone, private loggerService: LoggerService) {
        this.language = '';
        this.renderPageData();
    }

    ngAfterViewInit(): void {
        this.ngZone.runOutsideAngular(() =>
            setTimeout(() => document.getElementById('login-component-login-input').focus()
        ));
    }

    /**
     *  Executes the logIn service
     */
    logInUser(): void {
        let isSetUserId = this.myAccountLogin !== undefined && this.myAccountLogin.length > 0;
        let isSetPassword = this.password !== undefined && this.password.length > 0;
        this.message = '';

        if (isSetUserId && isSetPassword) {
            this.login.emit({
                language: this.language,
                login: this.myAccountLogin,
                password: this.password,
                remenberMe: this.myAccountRememberMe
            });
        } else {
            let error = '';
            if (!isSetUserId) {
                error += (this.mandatoryFieldError).replace('{0}', this.emailAddressLabel);
            }

            if (!isSetPassword) {
                if (error !== '') {
                    error +=  '<br>';
                }
                error += (this.mandatoryFieldError).replace('{0}', this.passwordLabel);
            }
            this.message = error;
            this.isLoginInProgress = false;
        }
    }

    /**
     * Execute the change language service
     */
    changeLanguage(lang: string): void {
        this.language = lang;
        this.renderPageData();
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        this.recoverPassword.emit();
    }

    /**
     * Renders all the labels, images, and placeholder values for the Log In page.
     */
    private renderPageData(): void {

        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe((data) => {

            // Translate labels and messages
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;

            this.loginLabel = dataI18n.Login;
            this.emailAddressLabel = 'emailAddress' === entity.authorizationType ? this.userIdOrEmailLabel = dataI18n['email-address'] :
                this.userIdOrEmailLabel = dataI18n['user-id'];

            this.passwordLabel = dataI18n.password;
            this.rememberMeLabel = dataI18n['remember-me'];
            this.loginButton = dataI18n['sign-in'].toUpperCase();
            this.forgotPasswordButton = dataI18n['get-new-password'];
            this.cancelButton = dataI18n.cancel;
            this.serverLabel = dataI18n.Server;
            this.mandatoryFieldError = dataI18n['error.form.mandatory'];
            this.communityLicenseInfoMessage = dataI18n['angular.login.component.community.licence.message'];
            this.resetPasswordSuccess = dataI18n['reset-password-success'];
            this.resetEmailMessage = dataI18n['a-new-password-has-been-sent-to-x'];

            // Set dotCMS Info
            this.dotcmscompanyLogo = entity.logo;
            this.dotcmsServerId = entity.serverId;
            this.dotcmslicenceLevel = entity.levelName;
            if ( this.dotcmslicenceLevel.indexOf('COMMUNITY') !== -1) {
                this.isCommunityLicense = true;
            } else {
                this.isCommunityLicense = false;
            }
            this.dotcmsVersion = entity.version;
            this.dotcmsBuildDateString = entity.buildDateString;

            // Configure languages
            if (this.languages.length === 0) {
                let currentLanguage = entity.currentLanguage;

                entity.languages.forEach(lang => {
                    this.languages.push({
                        label: lang.displayName,
                        value: lang.language + '_' + lang.country,
                    });
                });

                this.language = currentLanguage.language + '_' + currentLanguage.country;
            }

            if (this.passwordChanged) {
                this.message = this.resetPasswordSuccess;
            }
            if (this.resetEmailSent) {
                this.message = this.resetEmailMessage.replace('{0}', this.resetEmail);
            }
        }, (error) => {
             this.loggerService.debug(error);
        });
    }
}
