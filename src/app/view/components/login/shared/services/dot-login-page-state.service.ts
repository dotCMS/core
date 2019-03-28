import { Injectable } from '@angular/core';
import { DotLoginInformation } from '@models/dot-login';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoginService } from 'dotcms-js';
import { pluck, take, tap } from 'rxjs/operators';

export const LOGIN_LABELS = [
    'email-address',
    'user-id',
    'password',
    'remember-me',
    'sign-in',
    'get-new-password',
    'cancel',
    'Server',
    'error.form.mandatory',
    'angular.login.component.community.licence.message',
    'reset-password-success',
    'a-new-password-has-been-sent-to-x',
    'welcome-back',
    'forgot-password',
    'get-new-password',
    'cancel',
    'an-email-with-instructions-will-be-sent',
    'reset-password',
    'enter-password',
    're-enter-password',
    'change-password',
    'reset-password-confirmation-do-not-match',
    'message.forgot.password.password.updated'
];

@Injectable()
export class DotLoginPageStateService {
    private dotLoginInformation$: BehaviorSubject<DotLoginInformation> = new BehaviorSubject(null);

    constructor(private loginService: LoginService) {}

    get(): Observable<DotLoginInformation> {
        return this.dotLoginInformation$.asObservable();
    }

    set(lang: string): Observable<DotLoginInformation> {
        return this.loginService.getLoginFormInfo(lang, LOGIN_LABELS).pipe(
            take(1),
            pluck('bodyJsonObject'),
            tap((loginInfo: DotLoginInformation) => {
                this.dotLoginInformation$.next(loginInfo);
            })
        );
    }

    update(lang: string): void {
        this.loginService
            .getLoginFormInfo(lang, LOGIN_LABELS)
            .pipe(take(1), pluck('bodyJsonObject'))
            .subscribe((loginInfo: DotLoginInformation) => {
                this.dotLoginInformation$.next(loginInfo);
            });
    }
}
