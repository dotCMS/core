import { forkJoin, Observable, of } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, take } from 'rxjs/operators';

import { LoginService, type DotcmsConfigService, type LoggerService } from '@dotcms/dotcms-js';

import type { DotMessageService } from './dot-messages.service';
import type { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

/** Waits for legacy editor translations without preventing custom element bootstrap on failure. */
export function initializeLegacyTranslations(
    messageService: Pick<DotMessageService, 'init'>,
    configService: Pick<DotcmsConfigService, 'getConfig'>,
    _storage: Pick<DotLocalstorageService, 'getItem'>,
    logger: Pick<LoggerService, 'warn'>,
    loginService: Pick<LoginService, 'loadAuth' | 'currentUserLanguageId'> = inject(LoginService)
): Observable<void> {
    return forkJoin({
        auth: loginService.loadAuth().pipe(take(1)),
        config: configService.getConfig().pipe(
            take(1),
            catchError((error) => {
                logger.warn('Unable to load configuration; fetching uncached translations.', error);

                return of(null);
            })
        )
    }).pipe(
        switchMap(({ auth, config }) =>
            messageService.init({
                language:
                    auth.loginAsUser?.languageId ||
                    auth.sessionLanguageId ||
                    loginService.currentUserLanguageId ||
                    'default',
                languageBundleVersion: config?.languageBundleVersion,
                userId: auth.loginAsUser?.userId || auth.user?.userId || null
            })
        ),
        catchError((error) => {
            logger.warn('Unable to load translations; continuing editor bootstrap.', error);

            return of(void 0);
        })
    );
}
