import { Component, EventEmitter, Input, Output, ViewEncapsulation, OnInit } from '@angular/core';
import { LoginService, LoggerService } from 'dotcms-js';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-forgot-password-component',
    templateUrl: 'forgot-password.component.html'
})
export class ForgotPasswordComponent implements OnInit {
    @Input()
    message: string;
    @Output()
    cancel = new EventEmitter<any>();
    @Output()
    recoverPassword = new EventEmitter<string>();

    forgotPasswordLogin: string;
    cancelButton = '';
    forgotPasswordButton = '';
    forgotPasswordLabel = '';
    userIdOrEmailLabel = '';
    emailMandatoryFieldError = '';

    private forgotPasswordConfirmationMessage = '';
    private language = '';
    private i18nMessages: Array<string> = [
        'error.form.mandatory',
        'user-id',
        'email-address',
        'forgot-password',
        'get-new-password',
        'cancel',
        'an-email-with-instructions-will-be-sent'
    ];

    constructor(private loginService: LoginService, private loggerService: LoggerService) {}

    ngOnInit(): void {
        this.loadLabels();
    }

    /**
     * Executes the recover password service
     */
    ok(): void {
        if (confirm(this.forgotPasswordConfirmationMessage)) {
            this.recoverPassword.emit(this.forgotPasswordLogin);
        }
    }

    /**
     * Update the color and or image according to the values specified
     */
    private loadLabels(): void {
        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe(
            (data) => {
                // Translate labels and messages
                const dataI18n = data.i18nMessagesMap;
                const entity = data.entity;

                if ('emailAddress' === entity.authorizationType) {
                    this.userIdOrEmailLabel = dataI18n['email-address'];
                } else {
                    this.userIdOrEmailLabel = dataI18n['user-id'];
                }

                this.forgotPasswordLabel = dataI18n['forgot-password'];
                this.forgotPasswordButton = dataI18n['get-new-password'];
                this.cancelButton = dataI18n.cancel;
                this.forgotPasswordConfirmationMessage =
                    dataI18n['an-email-with-instructions-will-be-sent'];
                this.emailMandatoryFieldError = dataI18n['error.form.mandatory'].replace(
                    '{0}',
                    this.userIdOrEmailLabel
                );
            },
            (error) => {
                this.loggerService.error(error);
            }
        );
    }
}
