import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { AfterViewChecked, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { take, tap } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent
} from '@dotcms/ui';

import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

@Component({
    providers: [],
    selector: 'dot-reset-password-component',
    styleUrls: ['./reset-password.component.scss'],
    templateUrl: 'reset-password.component.html',
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule,
        ButtonModule,
        InputTextModule,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        DotFieldRequiredDirective
    ]
})
export class ResetPasswordComponent implements OnInit, AfterViewChecked {
    private fb = inject(UntypedFormBuilder);
    private loginService = inject(LoginService);
    dotLoginPageStateService = inject(DotLoginPageStateService);
    private dotRouterService = inject(DotRouterService);
    private route = inject(ActivatedRoute);
    private readonly cd = inject(ChangeDetectorRef);

    resetPasswordForm: UntypedFormGroup;
    loginInfo$: Observable<DotLoginInformation>;
    message = '';
    private passwordDontMatchMessage = '';

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
