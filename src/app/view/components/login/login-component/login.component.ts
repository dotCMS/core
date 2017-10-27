import { Component, EventEmitter, Input, NgZone, Output, ViewEncapsulation } from '@angular/core';
import { LoginData } from './login-container.component';
import { LoginService, LoggerService } from 'dotcms-js/dotcms-js';
import { DotLoadingIndicatorService } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-login-component',
    templateUrl: 'login.component.html'
}) /**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginComponent {
    @Input() isLoginInProgress = false;
    @Input() message = '';
    @Input() passwordChanged = false;
    @Input() resetEmailSent = false;
    @Input() resetEmail = '';

    @Output() recoverPassword = new EventEmitter<any>();
    @Output() login = new EventEmitter<LoginData>();

    public myAccountLogin: string;
    public password: string;
    public myAccountRememberMe = false;
    public language = '';

    public languages: Array<any> = [];

    // labels
    private cancelButton = '';
    public communityLicenseInfoMessage = '';
    public dotcmsBuildDateString = '';
    public dotcmscompanyLogo = '';
    public dotcmslicenceLevel = '';
    public dotcmsServerId = '';
    public dotcmsVersion = '';
    private emailAddressLabel = '';
    public forgotPasswordButton = '';
    public loginButton = '';
    public loginLabel = '';
    private mandatoryFieldError = '';
    public passwordLabel = '';
    public rememberMeLabel = '';
    private resetEmailMessage = '';
    private resetPasswordSuccess = '';
    public serverLabel = '';
    public userIdOrEmailLabel = '';

    public isCommunityLicense = true;

    private i18nMessages: Array<string> = [
        'Login',
        'email-address',
        'user-id',
        'password',
        'remember-me',
        'sign-in',
        'get-new-password',
        'cancel',
        'Server',
        'error.form.mandatory',
        'angular.login.component.community.licence.message',
        'reset-password-success',
        'a-new-password-has-been-sent-to-x'
    ];

    constructor(
        private loginService: LoginService,
        private ngZone: NgZone,
        private loggerService: LoggerService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService
    ) {
        this.language = '';
        this.renderPageData();
    }

    ngAfterViewInit(): void {
        this.ngZone.runOutsideAngular(() =>
            setTimeout(() => document.getElementById('login-component-login-input').focus())
        );
    }

    /**
     *  Executes the logIn service
     */
    logInUser(): void {
        const isSetUserId = this.myAccountLogin !== undefined && this.myAccountLogin.length > 0;
        const isSetPassword = this.password !== undefined && this.password.length > 0;
        this.message = '';

        if (isSetUserId && isSetPassword) {
            this.login.emit({
                language: this.language,
                login: this.myAccountLogin,
                password: this.password,
                rememberMe: this.myAccountRememberMe
            });
        } else {
            let error = '';
            if (!isSetUserId) {
                error += this.mandatoryFieldError.replace('{0}', this.emailAddressLabel);
            }

            if (!isSetPassword) {
                if (error !== '') {
                    error += '<br>';
                }
                error += this.mandatoryFieldError.replace('{0}', this.passwordLabel);
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
        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe(
            data => {
                // Translate labels and messages
                const dataI18n = data.i18nMessagesMap;
                const entity = data.entity;

                this.loginLabel = dataI18n.Login;
                this.emailAddressLabel =
                    'emailAddress' === entity.authorizationType
                        ? (this.userIdOrEmailLabel = dataI18n['email-address'])
                        : (this.userIdOrEmailLabel = dataI18n['user-id']);

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
                if (this.dotcmslicenceLevel.indexOf('COMMUNITY') !== -1) {
                    this.isCommunityLicense = true;
                } else {
                    this.isCommunityLicense = false;
                }
                this.dotcmsVersion = entity.version;
                this.dotcmsBuildDateString = entity.buildDateString;

                // Configure languages
                if (this.languages.length === 0) {
                    const currentLanguage = entity.currentLanguage;

                    entity.languages.forEach(lang => {
                        this.languages.push({
                            label: lang.displayName,
                            value: lang.language + '_' + lang.country
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
            },
            error => {
                this.loggerService.debug(error);
            }
        );
    }
}
