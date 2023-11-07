import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator';
import { of } from 'rxjs';

import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@dotcms/ui';

const INVALID_DATE_MSG = 'Invalid date';
const VALID_TIMESTAMP = 1701189800000;
const WRONG_TIMESTAMP = 1651337877000000;

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
            const EXPECTED_DATE = '11/28/2023, 11:43 AM';
            expect(spectator.service.getDateFromTimestamp(VALID_TIMESTAMP)).toBe(EXPECTED_DATE);
        });

        it('should return a string with correct date format using timestamp using `currentUserLanguageId` with es-ES', () => {
            const EXPECTED_DATE = '28/11/2023, 11:43 a. m.';

            loginService.currentUserLanguageId = 'es-ES';
            expect(spectator.service.getDateFromTimestamp(VALID_TIMESTAMP)).toBe(EXPECTED_DATE);
        });
    });
});
