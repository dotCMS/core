import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpCode, LoggerService, LoginService, User, DotLoginParams } from 'dotcms-js';
import { DotLoginInformation, DotLoginLanguage } from '@models/dot-login';
import { SelectItem } from 'primeng/api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { take, takeUntil, tap } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { ActivatedRoute, Params } from '@angular/router';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

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
    loginForm: FormGroup;
    languages: SelectItem[] = [];
    loginInfo$: Observable<DotLoginInformation>;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private loginService: LoginService,
        private fb: FormBuilder,
        private dotRouterService: DotRouterService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private loggerService: LoggerService,
        private route: ActivatedRoute,
        public loginPageStateService: DotLoginPageStateService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService.init(true);
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
        this.message = '';

        this.loginService
            .loginUser(this.loginForm.value as DotLoginParams)
            .pipe(take(1))
            .subscribe(
                (user: User) => {
                    this.message = '';
                    this.dotLoadingIndicatorService.hide();
                    this.dotRouterService.goToMain(user['editModeUrl']);
                    this.dotMessageService.setRelativeDateMessages(user.languageId);
                },
                (error: any) => {
                    if (this.isBadRequestOrUnathorized(error.status)) {
                        this.message = error.bodyJsonObject.errors[0].message;
                    } else {
                        this.loggerService.debug(error);
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
        this.dotMessageService.init(true, lang);
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
                this.message = loginInfo.i18nMessagesMap['reset-password-success'];
            } else if (params['resetEmailSent']) {
                this.message = loginInfo.i18nMessagesMap[
                    'a-new-password-has-been-sent-to-x'
                ].replace('{0}', params['resetEmail']);
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
}
