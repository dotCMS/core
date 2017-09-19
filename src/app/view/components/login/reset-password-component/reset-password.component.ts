import { Component, ViewEncapsulation, Input, Output, EventEmitter } from '@angular/core';
import { LoginService, LoggerService } from 'dotcms-js/dotcms-js';
import { ChangePasswordData } from './reset-password-container.component';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    providers: [],
    selector: 'dot-reset-password-component',
    styleUrls: [],
    templateUrl: 'reset-password.component.html'
})
export class ResetPasswordComponent {
    @Input() private token = '';
    @Input() public message = '';

    @Output() private changePassword = new EventEmitter<ChangePasswordData>();

    private language = '';

    // labels
    public resetPasswordLabel = '';
    public enterPasswordLabel = '';
    public confirmPasswordLabel = '';
    public changePasswordButton = '';

    // Message
    private resetPasswordSuccessMessage = '';
    private resetPasswordConfirmationDoNotMessage = '';
    private mandatoryFieldError = '';
    public passwordMandatoryFieldError = '';
    public confirmPasswordMandatoryFieldError = '';

    public password = '';
    public confirmPassword = '';

    private i18nMessages: Array<string> = [
        'error.form.mandatory',
        'reset-password',
        'enter-password',
        're-enter-password',
        'change-password',
        'reset-password-success',
        'reset-password-confirmation-do-not-match'
    ];

    constructor(private loginService: LoginService, private loggerService: LoggerService) {}

    ngOnInit(): void {
        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe(
            data => {
                const dataI18n = data.i18nMessagesMap;

                this.resetPasswordLabel = dataI18n['reset-password'];
                this.enterPasswordLabel = dataI18n['enter-password'];
                this.confirmPasswordLabel = dataI18n['re-enter-password'];
                this.changePasswordButton = dataI18n['change-password'];
                this.mandatoryFieldError = dataI18n['error.form.mandatory'];
                this.passwordMandatoryFieldError = this.mandatoryFieldError.replace(
                    '{0}',
                    this.enterPasswordLabel
                );
                this.confirmPasswordMandatoryFieldError = this.mandatoryFieldError.replace(
                    '{0}',
                    this.confirmPasswordLabel
                );
                this.resetPasswordConfirmationDoNotMessage =
                    dataI18n['reset-password-confirmation-do-not-match'];
                this.resetPasswordSuccessMessage = dataI18n['reset-password-success'];
            },
            error => {
                this.loggerService.error(error);
            }
        );
    }

    public ok(): void {
        if (this.password === this.confirmPassword) {
            this.changePassword.emit({
                password: this.password,
                token: this.token
            });
        } else {
            this.message = this.resetPasswordConfirmationDoNotMessage;
        }
    }

    // tslint:disable-next-line:no-unused-variable
    private cleanConfirmPassword(): void {
        this.clean();
        this.confirmPassword = '';
    }
    private clean(): void {
        this.message = '';
    }
}
