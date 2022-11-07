import { AfterViewChecked, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { take, tap } from 'rxjs/operators';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { LoginService } from '@dotcms/dotcms-js';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotLoginInformation } from '@dotcms/dotcms-models';

@Component({
    providers: [],
    selector: 'dot-reset-password-component',
    styleUrls: ['./reset-password.component.scss'],
    templateUrl: 'reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit, AfterViewChecked {
    resetPasswordForm: UntypedFormGroup;
    loginInfo$: Observable<DotLoginInformation>;
    message = '';
    private passwordDontMatchMessage = '';

    constructor(
        private fb: UntypedFormBuilder,
        private loginService: LoginService,
        public dotLoginPageStateService: DotLoginPageStateService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        private readonly cd: ChangeDetectorRef
    ) {}

    ngAfterViewChecked() {
        this.cd.detectChanges();
    }

    ngOnInit(): void {
        this.loginInfo$ = this.dotLoginPageStateService.get().pipe(
            take(1),
            tap((loginInfo: DotLoginInformation) => {
                this.passwordDontMatchMessage =
                    loginInfo.i18nMessagesMap['reset-password-confirmation-do-not-match'];
            })
        );

        this.resetPasswordForm = this.fb.group({
            password: ['', [Validators.required]],
            confirmPassword: ['', [Validators.required]]
        });
    }

    /**
     * Clean confirm password field value.
     *
     * @memberof ResetPasswordComponent
     */
    cleanConfirmPassword(): void {
        this.cleanMessage();
        this.resetPasswordForm.get('confirmPassword').setValue('');
    }

    /**
     * Clean the error message
     *
     * @memberof ResetPasswordComponent
     */
    cleanMessage(): void {
        this.message = '';
    }

    /**
     * Validate password change and make the request.
     *
     * @memberof ResetPasswordComponent
     */
    submit(): void {
        if (
            this.resetPasswordForm.valid &&
            this.resetPasswordForm.get('password').value ===
                this.resetPasswordForm.get('confirmPassword').value
        ) {
            this.cleanMessage();
            this.loginService
                .changePassword(
                    this.resetPasswordForm.get('password').value,
                    this.route.snapshot.paramMap.get('token')
                )
                .pipe(take(1))
                .subscribe(
                    () => {
                        this.dotRouterService.goToLogin({
                            queryParams: {
                                changedPassword: true
                            }
                        });
                    },
                    (response) => {
                        this.message = response.error?.errors[0]?.message;
                    }
                );
        } else {
            this.message = this.passwordDontMatchMessage;
        }
    }
}
