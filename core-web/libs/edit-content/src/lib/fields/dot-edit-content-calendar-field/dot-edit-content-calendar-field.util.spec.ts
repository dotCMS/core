import { describe, expect, it } from '@jest/globals';

import {
    CALENDAR_OPTIONS_PER_TYPE,
    convertToServerTimezoneForDisplay
} from './dot-edit-content-calendar-field.util';

import { SystemTimezone } from '@dotcms/dotcms-js';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

// Mock de timezone del servidor - formato real que viene del backend
const SERVER_TIMEZONE_MOCKS = {
    UTC: {
        id: 'UTC',
        label: 'Coordinated Universal Time (UTC)',
        offset: '0' // UTC tiene offset 0
    } as SystemTimezone,

    EST: {
        id: 'America/New_York',
        label: 'Eastern Standard Time (America/New_York)',
        offset: '-18000000' // -5 horas en milisegundos (-5 * 3600 * 1000)
    } as SystemTimezone,

    JST: {
        id: 'Asia/Tokyo',
        label: 'Japan Standard Time (Asia/Tokyo)',
        offset: '32400000' // +9 horas en milisegundos (9 * 3600 * 1000)
    } as SystemTimezone,

    GULF: {
        id: 'Asia/Dubai',
        label: 'Gulf Standard Time (Asia/Dubai)',
        offset: '14400000' // +4 horas en milisegundos (4 * 3600 * 1000)
    } as SystemTimezone
};

describe('DotEditContentCalendarFieldUtil - TDD Approach', () => {

    describe('CALENDAR_OPTIONS_PER_TYPE - Basic Configuration', () => {
        it('should exist and have configuration for all field types', () => {
            expect(CALENDAR_OPTIONS_PER_TYPE).toBeDefined();
            expect(CALENDAR_OPTIONS_PER_TYPE[FIELD_TYPES.DATE]).toBeDefined();
            expect(CALENDAR_OPTIONS_PER_TYPE[FIELD_TYPES.DATE_AND_TIME]).toBeDefined();
            expect(CALENDAR_OPTIONS_PER_TYPE[FIELD_TYPES.TIME]).toBeDefined();
        });

        it('should have correct basic structure for DATE field', () => {
            const dateConfig = CALENDAR_OPTIONS_PER_TYPE[FIELD_TYPES.DATE];
            expect(dateConfig).toHaveProperty('showTime');
            expect(dateConfig).toHaveProperty('timeOnly');
            expect(dateConfig).toHaveProperty('icon');
        });
    });

    describe('convertToServerTimezoneForDisplay - Timezone Conversion Tests', () => {

        it('should convert Toronto 10:00 AM (-0400) to Dubai 18:00 (same day)', () => {
            // Given: 10:00 AM Toronto time (-0400) = 14:00 UTC
            const torontoMorning = new Date('2025-06-05T10:00:00-04:00');

            // When: Converting to Dubai server timezone (+0400)
            const result = convertToServerTimezoneForDisplay(torontoMorning, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should show 18:00 Dubai time (14:00 UTC + 4 hours)
            expect(result.getUTCHours()).toBe(18);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(5); // Same day
        });

        it('should convert Toronto 17:00 (-0400) to Dubai 01:00 next day (+0400)', () => {
            // Given: 17:00 Toronto time (-0400) = 21:00 UTC
            const torontoEvening = new Date('2025-06-05T17:00:00-04:00');

            // When: Converting to Dubai server timezone (+0400)
            const result = convertToServerTimezoneForDisplay(torontoEvening, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should show 01:00 Dubai time next day (21:00 UTC + 4 hours = 25:00 = 01:00 next day)
            expect(result.getUTCHours()).toBe(1);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(6); // Next day
        });

        it('should convert Toronto 10:00 AM (-0400) to UTC 14:00 (no offset)', () => {
            // Given: 10:00 AM Toronto time (-0400) = 14:00 UTC
            const torontoMorning = new Date('2025-06-05T10:00:00-04:00');

            // When: Converting to UTC server timezone (offset 0)
            const result = convertToServerTimezoneForDisplay(torontoMorning, SERVER_TIMEZONE_MOCKS.UTC);

            // Then: Should remain 14:00 UTC (no offset applied)
            expect(result.getUTCHours()).toBe(14);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(5); // Same day
        });

        it('should convert Toronto 17:00 (-0400) to UTC 21:00 (no offset)', () => {
            // Given: 17:00 Toronto time (-0400) = 21:00 UTC
            const torontoEvening = new Date('2025-06-05T17:00:00-04:00');

            // When: Converting to UTC server timezone (offset 0)
            const result = convertToServerTimezoneForDisplay(torontoEvening, SERVER_TIMEZONE_MOCKS.UTC);

            // Then: Should remain 21:00 UTC (no offset applied)
            expect(result.getUTCHours()).toBe(21);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(5); // Same day
        });

        it('should convert Toronto 10:00 AM (-0400) to EST 09:00 (-0500)', () => {
            // Given: 10:00 AM Toronto time (-0400) = 14:00 UTC
            const torontoMorning = new Date('2025-06-05T10:00:00-04:00');

            // When: Converting to EST server timezone (-0500)
            const result = convertToServerTimezoneForDisplay(torontoMorning, SERVER_TIMEZONE_MOCKS.EST);

            // Then: Should show 09:00 EST (14:00 UTC - 5 hours)
            expect(result.getUTCHours()).toBe(9);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(5); // Same day
        });

        it('should convert Toronto 17:00 (-0400) to EST 16:00 (-0500)', () => {
            // Given: 17:00 Toronto time (-0400) = 21:00 UTC
            const torontoEvening = new Date('2025-06-05T17:00:00-04:00');

            // When: Converting to EST server timezone (-0500)
            const result = convertToServerTimezoneForDisplay(torontoEvening, SERVER_TIMEZONE_MOCKS.EST);

            // Then: Should show 16:00 EST (21:00 UTC - 5 hours)
            expect(result.getUTCHours()).toBe(16);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(5); // Same day
        });

        it('should convert Toronto 10:00 AM (-0400) to JST 23:00 (+0900)', () => {
            // Given: 10:00 AM Toronto time (-0400) = 14:00 UTC
            const torontoMorning = new Date('2025-06-05T10:00:00-04:00');

            // When: Converting to JST server timezone (+0900)
            const result = convertToServerTimezoneForDisplay(torontoMorning, SERVER_TIMEZONE_MOCKS.JST);

            // Then: Should show 23:00 JST (14:00 UTC + 9 hours)
            expect(result.getUTCHours()).toBe(23);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(5); // Same day
        });

        it('should convert Toronto 17:00 (-0400) to JST 06:00 next day (+0900)', () => {
            // Given: 17:00 Toronto time (-0400) = 21:00 UTC
            const torontoEvening = new Date('2025-06-05T17:00:00-04:00');

            // When: Converting to JST server timezone (+0900)
            const result = convertToServerTimezoneForDisplay(torontoEvening, SERVER_TIMEZONE_MOCKS.JST);

            // Then: Should show 06:00 JST next day (21:00 UTC + 9 hours = 30:00 = 06:00 next day)
            expect(result.getUTCHours()).toBe(6);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCDate()).toBe(6); // Next day
        });

    });

});
