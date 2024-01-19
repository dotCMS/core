import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';

import { DotFormatDateService } from './dot-format-date.service';

const INVALID_DATE_MSG = 'Invalid date';
const VALID_TIMESTAMP = 1701189800000;
const WRONG_TIMESTAMP = 1651337877000000;
const DateFormatOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
    timeZone: 'UTC'
};

describe('DotFormatDateService', () => {
    let spectator: SpectatorService<DotFormatDateService>;
    let loginService: SpyObject<LoginService>;
    const createService = createServiceFactory({
        service: DotFormatDateService,
        providers: [
            mockProvider(DotcmsConfigService, {
                getSystemTimeZone: () => of('idk')
            }),
            mockProvider(LoginService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        loginService = spectator.inject(LoginService);
        loginService.currentUserLanguageId = 'en-US';
    });

    describe('getDateFromTimestamp', () => {
        it('should return `Invalid date` when is not a timestamp', () => {
            expect(spectator.service.getDateFromTimestamp(WRONG_TIMESTAMP)).toContain(
                INVALID_DATE_MSG
            );
        });

        it('should return a string date using timestamp using `currentUserLanguageId`with us-US', () => {
            const EXPECTED_DATE = '11/28/2023, 04:43 PM';
            expect(spectator.service.getDateFromTimestamp(VALID_TIMESTAMP, DateFormatOptions)).toBe(
                EXPECTED_DATE
            );
        });

        it('should return a string with correct date format using timestamp using `currentUserLanguageId` with es-ES', () => {
            const EXPECTED_DATE = '28/11/2023, 04:43 p. m.';

            loginService.currentUserLanguageId = 'es-ES';
            expect(spectator.service.getDateFromTimestamp(VALID_TIMESTAMP, DateFormatOptions)).toBe(
                EXPECTED_DATE
            );
        });
    });
});
