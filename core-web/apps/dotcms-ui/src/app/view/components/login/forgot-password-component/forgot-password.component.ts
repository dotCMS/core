import { Observable } from 'rxjs';

import { Component, OnInit, ViewEncapsulation, inject } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { NavigationExtras } from '@angular/router';

import { take, tap } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';

import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-forgot-password-component',
    templateUrl: 'forgot-password.component.html',
    styleUrls: ['./forgot-password.component.scss'],
    standalone: false
})
export class ForgotPasswordComponent implements OnInit {
    private fb = inject(UntypedFormBuilder);
    loginPageStateService = inject(DotLoginPageStateService);
    private dotRouterService = inject(DotRouterService);
    private loginService = inject(LoginService);

    message = '';

    forgotPasswordForm: UntypedFormGroup;
    loginInfo$: Observable<DotLoginInformation>;

    private forgotPasswordConfirmationMessage = '';

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
