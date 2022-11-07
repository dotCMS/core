import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { take, tap } from 'rxjs/operators';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { LoginService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { NavigationExtras } from '@angular/router';
import { DotLoginInformation } from '@dotcms/dotcms-models';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-forgot-password-component',
    templateUrl: 'forgot-password.component.html',
    styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {
    message = '';

    forgotPasswordForm: UntypedFormGroup;
    loginInfo$: Observable<DotLoginInformation>;

    private forgotPasswordConfirmationMessage = '';

    constructor(
        private fb: UntypedFormBuilder,
        public loginPageStateService: DotLoginPageStateService,
        private dotRouterService: DotRouterService,
        private loginService: LoginService
    ) {}

    ngOnInit(): void {
        this.loginInfo$ = this.loginPageStateService.get().pipe(
            take(1),
            tap((loginInfo: DotLoginInformation) => {
                this.forgotPasswordConfirmationMessage =
                    loginInfo.i18nMessagesMap['an-email-with-instructions-will-be-sent'];
            })
        );
        this.forgotPasswordForm = this.fb.group({
            login: ['', [Validators.required]]
        });
    }

    /**
     * Executes the recover password service
     *
     * @memberof ForgotPasswordComponent
     */
    submit(): void {
        if (confirm(this.forgotPasswordConfirmationMessage)) {
            this.message = '';
            this.loginService
                .recoverPassword(this.forgotPasswordForm.get('login').value)
                .pipe(take(1))
                .subscribe(
                    () => {
                        this.goToLogin({
                            queryParams: {
                                resetEmailSent: true,
                                resetEmail: this.forgotPasswordForm.get('login').value
                            }
                        });
                    },
                    (response) => {
                        this.message = response.error?.errors[0]?.message;
                    }
                );
        }
    }

    /**
     * Executes the recover password service
     *
     * @param NavigationExtras parameters
     * @memberof ForgotPasswordComponent
     */
    goToLogin(parameters?: NavigationExtras): void {
        this.dotRouterService.goToLogin(parameters);
    }
}
