
import {Component, EventEmitter, Inject, Input, NgZone, Output, ViewEncapsulation} from '@angular/core';


// angular material imports
import {MdButton} from '@angular2-material/button';
import {MdCheckbox} from '@angular2-material/checkbox/checkbox';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {DotCMSHttpResponse} from '../../../../../api/services/dotcms-http-response';
import {LoginData} from './login-container';
import {MD_PROGRESS_CIRCLE_DIRECTIVES} from '@angular2-material/progress-circle';
import {MdToolbar} from '@angular2-material/toolbar';
import {LoginService} from '../../../../../api/services/login-service';
import {CapitalizePipe} from '../../../../../api/pipes/capitalize-pipe';

@Component({
    directives: [MdButton,MdCheckbox, MD_INPUT_DIRECTIVES, MD_PROGRESS_CIRCLE_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [CapitalizePipe],
    providers: [],
    selector: 'dot-login-component',
    styleUrls: [],
    templateUrl: ['login-component.html'],
})

/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginComponent {

    @Input() isLoginInProgress: boolean = false;
    @Input()  message: string = '';

    @Output() recoverPassword  = new EventEmitter<>();
    @Output() login  = new EventEmitter<LoginData>();
    private myAccountLogin: string;
    private password: string;
    private myAccountRememberMe: boolean = false;
    private language: string = '';

    languages: Array<string> = [];

    // labels
    loginLabel: string = '';
    emailAddressLabel: string = '';
    userIdOrEmailLabel: string = '';
    passwordLabel: string = '';
    rememberMeLabel: string = '';
    loginButton: string = '';
    forgotPasswordButton: string = '';
    cancelButton: string = '';
    serverLabel: string = '';
    dotcmscompanyLogo: string = '';
    dotcmsServerId: string = '';
    dotcmslicenceLevel: string = '';
    dotcmsVersion: string = '';
    dotcmsBuildDateString: string = '';
    mandatoryFieldError: string = '';
    communityLicenseInfoMessage: string = '';

    isCommunityLicense: boolean = true;


   private i18nMessages: Array<string> = [ 'Login', 'email-address', 'user-id', 'password', 'remember-me', 'sign-in',
       'get-new-password', 'cancel', 'Server', 'error.form.mandatory',
       'angular.login.component.community.licence.message'];

    constructor(private loginService: LoginService, private ngZone: NgZone) {
        this.renderPageData();
        // TODO: Change in the future once the Angular autofocus directive works correctly.
        this.ngZone.runOutsideAngular(() => {
            setTimeout(() => document.getElementById('md-input-0-input').focus(), 0);
        });
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
                login: this.myAccountLogin,
                password: this.password,
                remenberMe: this.myAccountRememberMe,
                language: this.language
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
     * Renders all the labels, images, and placeholder values for the Log In page.
     */
    private renderPageData(): void {

        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe((data) => {

            // Translate labels and messages
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;

            this.loginLabel = dataI18n.Login;
            this.emailAddressLabel = dataI18n['email-address'];
            if ('emailAddress' === entity.authorizationType) {
                this.userIdOrEmailLabel = dataI18n['email-address'];
            } else {
                this.userIdOrEmailLabel = dataI18n['user-id'];
            }
            this.passwordLabel = dataI18n.password;
            this.rememberMeLabel = dataI18n['remember-me'];
            this.loginButton = dataI18n['sign-in'].toUpperCase();
            this.forgotPasswordButton = dataI18n['get-new-password'];
            this.cancelButton = dataI18n.cancel;
            this.serverLabel = dataI18n.Server;
            this.mandatoryFieldError = dataI18n['error.form.mandatory'];
            this.communityLicenseInfoMessage = dataI18n['angular.login.component.community.licence.message'];


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
        }, (error) => {
             console.log(error);
        });
    }


    /**
     * Display the forgot password card
     */
    showForgotPassword(): void {
        this.recoverPassword.emit();
    }
}
