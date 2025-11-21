import { Observable, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import {
    FormControl,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators,
    FormsModule,
    ReactiveFormsModule
} from '@angular/forms';
import { ActivatedRoute, Params, RouterLink } from '@angular/router';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { take, takeUntil, tap } from 'rxjs/operators';

import { DotMessageService, DotRouterService, DotFormatDateService } from '@dotcms/data-access';
import { DotLoginParams, HttpCode, LoggerService, LoginService, User } from '@dotcms/dotcms-js';
import { DotLoginInformation, DotLoginLanguage } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent
} from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotDirectivesModule } from '../../../../shared/dot-directives.module';
import { SharedModule } from '../../../../shared/shared.module';
import { DotLoadingIndicatorComponent } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.component';
import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

@Component({
    selector: 'dot-login-component',
    templateUrl: './dot-login.component.html',
    styleUrls: ['./dot-login.component.scss'],
    imports: [
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        CheckboxModule,
        SelectModule,
        InputTextModule,
        SharedModule,
        DotLoadingIndicatorComponent,
        DotDirectivesModule,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        DotFieldRequiredDirective,
        RouterLink
    ]
})
/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class DotLoginComponent implements OnInit, OnDestroy {
    private loginService = inject(LoginService);
    private fb = inject(UntypedFormBuilder);
    private dotRouterService = inject(DotRouterService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private loggerService = inject(LoggerService);
    private route = inject(ActivatedRoute);
    loginPageStateService = inject(DotLoginPageStateService);
    private dotMessageService = inject(DotMessageService);
    private dotFormatDateService = inject(DotFormatDateService);

    message = '';
    isError = false;
    loginForm: UntypedFormGroup;
    languages: SelectItem[] = [];
    loginInfo$: Observable<DotLoginInformation>;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit() {
        this.loginForm = this.fb.group({
            login: ['', [Validators.required]],
            language: [''],
            password: ['', [Validators.required]],
            rememberMe: false,
            backEndLogin: true
        });

        this.loginInfo$ = this.loginPageStateService.get().pipe(
            takeUntil(this.destroy$),
            tap((loginInfo: DotLoginInformation) => {
                this.setInitialFormValues(loginInfo);
            })
        );
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
    /**
     *  Executes the logIn service
     *
     *  @memberof DotLoginComponent
     */
    logInUser(): void {
        this.setFromState(true);
        this.dotLoadingIndicatorService.show();
        this.setMessage('');
        this.loginService
            .loginUser(this.loginForm.value as DotLoginParams)
            .pipe(take(1))
            .subscribe(
                (user: User) => {
                    this.setMessage('');
                    this.dotLoadingIndicatorService.hide();
                    this.dotRouterService.goToMain(user['editModeUrl']);
                    this.dotFormatDateService.setLang(user.languageId);
                },
                (res: HttpErrorResponse) => {
                    if (this.isBadRequestOrUnathorized(res.status)) {
                        this.setMessage(res.error.errors[0].message, true);
                    } else {
                        this.loggerService.debug(res);
                    }

                    this.setFromState(false);
                    this.dotLoadingIndicatorService.hide();
                }
            );
    }

    /**
     * Call the service to update the language
     *
     * @memberof DotLoginComponent
     */
    onLanguageChange(lang: string): void {
        this.loginPageStateService.update(lang);
        this.dotMessageService.init({ language: lang });
    }

    private setInitialFormValues(loginInfo: DotLoginInformation): void {
        this.loginForm
            .get('language')
            .setValue(this.getLanguageFormatted(loginInfo.entity.currentLanguage));
        this.setLanguageItems(loginInfo.entity.languages);
        this.setInitialMessage(loginInfo);
    }

    private isEmail(potentialEmail: string): boolean {
        return !!new FormControl(potentialEmail, Validators.email).errors?.email;
    }

    private setInitialMessage(loginInfo: DotLoginInformation): void {
        this.route.queryParams.pipe(take(1)).subscribe((params: Params) => {
            if (params['changedPassword']) {
                this.setMessage(loginInfo.i18nMessagesMap['reset-password-success']);
            } else if (params['resetEmailSent'] && !this.isEmail(params['resetEmail'])) {
                this.setMessage(
                    loginInfo.i18nMessagesMap['a-new-password-has-been-sent-to-x'].replace(
                        '{0}',
                        params['resetEmail']
                    )
                );
            }
        });
    }

    private setLanguageItems(languages: DotLoginLanguage[]): void {
        this.languages =
            this.languages.length === 0
                ? (this.languages = languages.map((lang: DotLoginLanguage) => ({
                      label: lang.displayName,
                      value: this.getLanguageFormatted(lang)
                  })))
                : this.languages;
    }

    private getLanguageFormatted(lang: DotLoginLanguage): string {
        return lang.language + '_' + lang.country;
    }

    private setFromState(disable: boolean): void {
        if (disable) {
            this.loginForm.disable();
        } else {
            this.loginForm.enable();
        }
    }

    private isBadRequestOrUnathorized(status: number) {
        return status === HttpCode.BAD_REQUEST || status === HttpCode.UNAUTHORIZED;
    }

    private setMessage(message: string, error?: boolean): void {
        this.message = message;
        this.isError = !!error;
    }
}
