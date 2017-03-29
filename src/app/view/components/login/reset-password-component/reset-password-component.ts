import {Component, ViewEncapsulation, Input, Output, EventEmitter} from '@angular/core';
import {LoginService} from '../../../../api/services/login-service';

// angular material imports
import {ChangePasswordData} from './reset-password-container';
import {LoggerService} from '../../../../api/services/logger.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    providers: [],
    selector: 'dot-reset-password-component',
    styleUrls: [],
    templateUrl: 'reset-password-component.html',
})

export class ResetPasswordComponent {

    @Input() private token = '';
    @Input() private message = '';

    @Output() private changePassword  = new EventEmitter<ChangePasswordData>();

    private language = '';

    // labels
    private resetPasswordLabel = '';
    private enterPasswordLabel = '';
    private confirmPasswordLabel = '';
    private changePasswordButton = '';

    // Message
    private resetPasswordSuccessMessage = '';
    private resetPasswordConfirmationDoNotMessage = '';
    private mandatoryFieldError = '';
    private passwordMandatoryFieldError = '';
    private confirmPasswordMandatoryFieldError = '';

    private password = '';
    private confirmPassword = '';

    private i18nMessages: Array<string> = [ 'error.form.mandatory', 'reset-password', 'enter-password', 're-enter-password', 'change-password', 'reset-password-success', 'reset-password-confirmation-do-not-match'];

    constructor(private loginService: LoginService, private loggerService: LoggerService) {}

    ngOnInit(): void {
        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe((data) => {
            let dataI18n = data.i18nMessagesMap;

            this.resetPasswordLabel = dataI18n['reset-password'];
            this.enterPasswordLabel = dataI18n['enter-password'];
            this.confirmPasswordLabel = dataI18n['re-enter-password'];
            this.changePasswordButton = dataI18n['change-password'];
            this.mandatoryFieldError = dataI18n['error.form.mandatory'];
            this.passwordMandatoryFieldError = (this.mandatoryFieldError).replace('{0}', this.enterPasswordLabel);
            this.confirmPasswordMandatoryFieldError = (this.mandatoryFieldError).replace('{0}', this.confirmPasswordLabel);
            this.resetPasswordConfirmationDoNotMessage = dataI18n['reset-password-confirmation-do-not-match'];
            this.resetPasswordSuccessMessage = dataI18n['reset-password-success'];
        }, (error) => {
                this.loggerService.error(error);
        });
    }

    public ok(): void {
        if (this.password === this.confirmPassword) {
            this.changePassword.emit({
                password: this.password,
                token: this.token
            });
        }else {
            this.message = this.resetPasswordConfirmationDoNotMessage;
        }
    }

    private cleanConfirmPassword(): void {
        this.clean();
        this.confirmPassword = '';
    }
    private clean(): void {
        this.message = '';
    }
}
