import { BehaviorSubject, Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter, map, take, tap } from 'rxjs/operators';

import { LoginService } from '@dotcms/dotcms-js';
import { DotLoginInformation } from '@dotcms/dotcms-models';

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
    'welcome-login',
    'forgot-password',
    'cancel',
    'an-email-with-instructions-will-be-sent',
    'reset-password',
    'enter-password',
    're-enter-password',
    'change-password',
    'reset-password-confirmation-do-not-match',
    'message.forgot.password.password.updated',
    'Logout',
    'message.successfully.logout'
];

@Injectable()
export class DotLoginPageStateService {
    private loginService = inject(LoginService);

    private dotLoginInformation$: BehaviorSubject<DotLoginInformation> = new BehaviorSubject(null);

    get(): Observable<DotLoginInformation> {
        return this.dotLoginInformation$.asObservable();
    }

    set(lang: string): Observable<DotLoginInformation> {
        return this.loginService.getLoginFormInfo(lang, LOGIN_LABELS).pipe(
            take(1),
            filter((loginInfo: DotLoginInformation) => !!loginInfo),
            map((loginInfo: DotLoginInformation) => {
                return {
                    ...loginInfo,
                    i18nMessagesMap: {
                        ...loginInfo.i18nMessagesMap,
                        emailAddressLabel: this.getUserNameLabel(loginInfo)
                    }
                };
            }),
            tap((loginInfo: DotLoginInformation) => {
                this.dotLoginInformation$.next(loginInfo);
            })
        );
    }

    update(lang: string): void {
        this.set(lang).pipe(take(1)).subscribe();
    }

    private getUserNameLabel(loginInfo: DotLoginInformation): string {
        if (!loginInfo?.entity || !loginInfo?.i18nMessagesMap) {
            return '';
        }

        return loginInfo.entity.authorizationType === 'emailAddress'
            ? loginInfo.i18nMessagesMap['email-address']
            : loginInfo.i18nMessagesMap['user-id'];
    }
}
