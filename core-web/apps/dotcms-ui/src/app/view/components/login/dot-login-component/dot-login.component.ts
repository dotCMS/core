import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpCode, LoggerService, LoginService, User, DotLoginParams } from '@dotcms/dotcms-js';
import { SelectItem } from 'primeng/api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { take, takeUntil, tap } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { ActivatedRoute, Params } from '@angular/router';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotLoginInformation, DotLoginLanguage } from '@dotcms/dotcms-models';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
    selector: 'dot-login-component',
    templateUrl: './dot-login.component.html',
    styleUrls: ['./dot-login.component.scss']
})
/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class DotLoginComponent implements OnInit, OnDestroy {
    message = '';
    isError = false;
    loginForm: UntypedFormGroup;
    languages: SelectItem[] = [];
    loginInfo$: Observable<DotLoginInformation>;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private loginService: LoginService,
        private fb: UntypedFormBuilder,
        private dotRouterService: DotRouterService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private loggerService: LoggerService,
        private route: ActivatedRoute,
        public loginPageStateService: DotLoginPageStateService,
        private dotMessageService: DotMessageService,
        private dotFormatDateService: DotFormatDateService
    ) {}

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

    /**
     * Display the forgot password card
     *
     * @memberof DotLoginComponent
     */
    goToForgotPassword(): void {
        this.dotRouterService.goToForgotPassword();
    }

    private setInitialFormValues(loginInfo: DotLoginInformation): void {
        this.loginForm
            .get('language')
            .setValue(this.getLanguageFormatted(loginInfo.entity.currentLanguage));
        this.setLanguageItems(loginInfo.entity.languages);
        this.setInitialMessage(loginInfo);
    }

    private setInitialMessage(loginInfo: DotLoginInformation): void {
        this.route.queryParams.pipe(take(1)).subscribe((params: Params) => {
            if (params['changedPassword']) {
                this.setMessage(loginInfo.i18nMessagesMap['reset-password-success']);
            } else if (params['resetEmailSent']) {
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
