import { Observable, of } from 'rxjs';

import { catchError, switchMap, take } from 'rxjs/operators';

import type { DotcmsConfigService, LoggerService } from '@dotcms/dotcms-js';

import type { DotMessageService } from './dot-messages.service';
import type { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

/** Waits for legacy editor translations without preventing custom element bootstrap on failure. */
export function initializeLegacyTranslations(
    messageService: Pick<DotMessageService, 'init'>,
    configService: Pick<DotcmsConfigService, 'getConfig'>,
    storage: Pick<DotLocalstorageService, 'getItem'>,
    logger: Pick<LoggerService, 'warn'>
): Observable<void> {
    let language = 'default';
    try {
        const cachedLanguage = storage.getItem<string>('dotMessagesKeys-lang');
        if (typeof cachedLanguage === 'string' && /^[a-zA-Z][a-zA-Z0-9_-]*$/.test(cachedLanguage)) {
            language = cachedLanguage;
        }
    } catch (error) {
        logger.warn('Unable to read cached translation locale; using default.', error);
    }

    return configService.getConfig().pipe(
        take(1),
        catchError((error) => {
            logger.warn('Unable to load configuration; fetching uncached translations.', error);

            return of(null);
        }),
        switchMap((config) =>
            messageService.init({
                language,
                languageBundleVersion: config?.languageBundleVersion
            })
        ),
        catchError((error) => {
            logger.warn('Unable to load translations; continuing editor bootstrap.', error);

            return of(void 0);
        })
    );
}
