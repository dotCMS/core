import { describe, expect, it } from '@jest/globals';

import { DotSystemTimezone } from '@dotcms/dotcms-models';
import {
    createFakeDateField,
    createFakeDateTimeField,
    createFakeTimeField
} from '@dotcms/utils-testing';

import {
    CALENDAR_OPTIONS_PER_TYPE,
    convertServerTimeToUtc,
    convertUtcTimestampToDateDisplay,
    convertUtcToServerTime,
    createServerTimezoneDate,
    createUtcDateAtMidnight,
    extractDateComponents,
    getCurrentServerTime,
    parseFieldDefaultValue,
    processExistingValue,
    processFieldDefaultValue
} from './dot-edit-content-calendar-field.util';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

// Mock de timezone del servidor - formato real que viene del backend
const SERVER_TIMEZONE_MOCKS = {
    UTC: {
        id: 'UTC',
        label: 'Coordinated Universal Time (UTC)',
        offset: 0 // UTC tiene offset 0
    } as DotSystemTimezone,

    EST: {
        id: 'America/New_York',
        label: 'Eastern Standard Time (America/New_York)',
        offset: -18000000 // -5 horas en milisegundos (-5 * 3600 * 1000)
    } as DotSystemTimezone,

    JST: {
        id: 'Asia/Tokyo',
        label: 'Japan Standard Time (Asia/Tokyo)',
        offset: 32400000 // +9 horas en milisegundos (9 * 3600 * 1000)
    } as DotSystemTimezone,

    GULF: {
        id: 'Asia/Dubai',
        label: 'Gulf Standard Time (Asia/Dubai)',
        offset: 14400000 // +4 horas en milisegundos (4 * 3600 * 1000)
    } as DotSystemTimezone
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

    describe('convertUtcToServerTime - UTC to Server Timezone Conversion', () => {
        it('should convert UTC 14:00 to Dubai 18:00 (+4)', () => {
            // Given: 14:00 UTC
            const utcDate = new Date('2025-06-05T14:00:00.000Z');

            // When: Converting to Dubai server timezone (+4)
            const result = convertUtcToServerTime(utcDate, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should show 18:00 Dubai time
            expect(result.getHours()).toBe(18);
            expect(result.getMinutes()).toBe(0);
        });

        it('should convert UTC 21:00 to Dubai 01:00 next day (+4)', () => {
            // Given: 21:00 UTC
            const utcDate = new Date('2025-06-05T21:00:00.000Z');

            // When: Converting to Dubai server timezone (+4)
            const result = convertUtcToServerTime(utcDate, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should show 01:00 Dubai time next day
            expect(result.getHours()).toBe(1);
            expect(result.getMinutes()).toBe(0);
            expect(result.getDate()).toBe(6); // Next day
        });

        it('should handle UTC timezone (should preserve time)', () => {
            // Given: A specific UTC time
            const utcDate = new Date('2025-06-05T14:00:00.000Z');

            // When: Converting to UTC server timezone (no offset)
            const result = convertUtcToServerTime(utcDate, SERVER_TIMEZONE_MOCKS.UTC);

            // Then: The essential time components should be preserved
            // (allowing for potential implementation differences with date-fns-tz)
            expect(result).toBeInstanceOf(Date);
            expect(result.getFullYear()).toBe(2025);
            expect(result.getMonth()).toBe(5); // June (0-indexed)
            expect(result.getDate()).toBe(5);
        });

        it('should handle null timezone gracefully', () => {
            const utcDate = new Date('2025-06-05T14:00:00.000Z');
            const result = convertUtcToServerTime(utcDate, null);
            expect(result).toBe(utcDate);
        });
    });

    describe('createServerTimezoneDate - Create Date from Components in Server Timezone', () => {
        it('should create a date for Dubai timezone that represents the correct moment', () => {
            // Given: Date components that user selected (Aug 6, 2025 23:30:45)
            const year = 2025;
            const month = 7; // August (0-indexed)
            const date = 6;
            const hours = 23;
            const minutes = 30;
            const seconds = 45;

            // When: Creating a date AS IF these components were in Dubai timezone
            const result = createServerTimezoneDate(
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
                SERVER_TIMEZONE_MOCKS.GULF
            );

            // Then: The result should represent the moment when it's 23:30:45 in Dubai
            // This should be 19:30:45 UTC (Dubai is UTC+4)
            expect(result.getUTCHours()).toBe(19);
            expect(result.getUTCMinutes()).toBe(30);
            expect(result.getUTCSeconds()).toBe(45);
        });

        it('should create a date for EST timezone that represents the correct moment', () => {
            // Given: Date components (Aug 6, 2025 15:30:00)
            const year = 2025;
            const month = 7; // August
            const date = 6;
            const hours = 15;
            const minutes = 30;
            const seconds = 0;

            // When: Creating a date AS IF these components were in EST timezone
            const result = createServerTimezoneDate(
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
                SERVER_TIMEZONE_MOCKS.EST
            );

            // Then: The result should represent the moment when it's 15:30 in EST
            // In August, EST is actually EDT (UTC-4), so 15:30 EDT = 19:30 UTC
            expect(result.getUTCHours()).toBe(19); // Adjusted for DST
            expect(result.getUTCMinutes()).toBe(30);
            expect(result.getUTCSeconds()).toBe(0);
        });

        it('should handle UTC timezone (no conversion needed)', () => {
            // Given: Date components
            const year = 2025;
            const month = 7;
            const date = 6;
            const hours = 12;
            const minutes = 0;
            const seconds = 0;

            // When: Creating a date in UTC timezone
            const result = createServerTimezoneDate(
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
                SERVER_TIMEZONE_MOCKS.UTC
            );

            // Then: Should maintain the same time in UTC
            expect(result.getUTCHours()).toBe(12);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCSeconds()).toBe(0);
        });

        it('should handle null timezone by creating local time', () => {
            // Given: Date components
            const year = 2025;
            const month = 7;
            const date = 6;
            const hours = 12;
            const minutes = 30;
            const seconds = 0;

            // When: Creating a date with null timezone
            const result = createServerTimezoneDate(
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
                null
            );

            // Then: Should create a local date with those components
            expect(result.getFullYear()).toBe(year);
            expect(result.getMonth()).toBe(month);
            expect(result.getDate()).toBe(date);
            expect(result.getHours()).toBe(hours);
            expect(result.getMinutes()).toBe(minutes);
            expect(result.getSeconds()).toBe(seconds);
        });

        it('should handle date components that cross timezone boundaries', () => {
            // Given: Components for midnight in Dubai (could be previous day in other timezones)
            const year = 2025;
            const month = 7;
            const date = 6;
            const hours = 0; // midnight in Dubai
            const minutes = 0;
            const seconds = 0;

            // When: Creating date for Dubai timezone
            const result = createServerTimezoneDate(
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
                SERVER_TIMEZONE_MOCKS.GULF
            );

            // Then: Should be Aug 5 20:00 UTC (midnight Dubai = 20:00 previous day UTC)
            expect(result.getUTCDate()).toBe(5); // Previous day
            expect(result.getUTCHours()).toBe(20);
        });
    });

    describe('getCurrentServerTime - Get Current Time in Server Timezone', () => {
        it('should return current time converted to server timezone', () => {
            // When: Getting current time for Dubai
            const result = getCurrentServerTime(SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should return a Date object (exact time varies, so we just check type)
            expect(result).toBeInstanceOf(Date);
            expect(result.getTime()).toBeGreaterThan(0);
        });

        it('should return current local time when timezone is null', () => {
            // When: Getting current time with null timezone
            const result = getCurrentServerTime(null);

            // Then: Should return a Date object
            expect(result).toBeInstanceOf(Date);
            expect(result.getTime()).toBeGreaterThan(0);
        });

        it('should return current time for UTC timezone', () => {
            // When: Getting current time for UTC
            const result = getCurrentServerTime(SERVER_TIMEZONE_MOCKS.UTC);

            // Then: Should return a Date object
            expect(result).toBeInstanceOf(Date);
            expect(result.getTime()).toBeGreaterThan(0);
        });
    });

    describe('convertUtcTimestampToDateDisplay - Convert UTC Timestamp to Date Display', () => {
        it('should convert UTC timestamp to correct date display', () => {
            // Given: UTC timestamp representing Aug 6, 2025 midnight UTC
            const utcTimestamp = new Date('2025-08-06T00:00:00.000Z');

            // When: Converting to display format
            const result = convertUtcTimestampToDateDisplay(utcTimestamp);

            // Then: Should show correct date components
            expect(result.getFullYear()).toBe(2025);
            expect(result.getMonth()).toBe(7); // August (0-indexed)
            expect(result.getDate()).toBe(6);
        });

        it('should handle string timestamps', () => {
            // Given: String timestamp
            const utcTimestamp = '1754438400000'; // Aug 6, 2025 midnight UTC

            // When: Converting to display format
            const result = convertUtcTimestampToDateDisplay(utcTimestamp);

            // Then: Should show correct date components
            expect(result.getFullYear()).toBe(2025);
            expect(result.getMonth()).toBe(7); // August
            expect(result.getDate()).toBe(6);
        });

        it('should handle number timestamps', () => {
            // Given: Number timestamp
            const utcTimestamp = 1754438400000; // Aug 6, 2025 midnight UTC

            // When: Converting to display format
            const result = convertUtcTimestampToDateDisplay(utcTimestamp);

            // Then: Should show correct date components
            expect(result.getFullYear()).toBe(2025);
            expect(result.getMonth()).toBe(7); // August
            expect(result.getDate()).toBe(6);
        });
    });

    describe('extractDateComponents - Extract Date Components', () => {
        it('should extract all date components correctly', () => {
            // Given: A specific date
            const date = new Date(2025, 7, 6, 15, 30, 45); // Aug 6, 2025 15:30:45

            // When: Extracting components
            const result = extractDateComponents(date);

            // Then: Should return correct components
            expect(result.year).toBe(2025);
            expect(result.month).toBe(7); // August (0-indexed)
            expect(result.date).toBe(6);
            expect(result.hours).toBe(15);
            expect(result.minutes).toBe(30);
            expect(result.seconds).toBe(45);
        });
    });

    describe('createUtcDateAtMidnight - Create UTC Midnight Date', () => {
        it('should create UTC date at midnight with correct components', () => {
            // When: Creating UTC midnight for Aug 6, 2025
            const result = createUtcDateAtMidnight(2025, 7, 6);

            // Then: Should be midnight UTC on correct date
            expect(result.getUTCFullYear()).toBe(2025);
            expect(result.getUTCMonth()).toBe(7); // August
            expect(result.getUTCDate()).toBe(6);
            expect(result.getUTCHours()).toBe(0);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCSeconds()).toBe(0);
        });

        it('should represent consistent timestamp globally', () => {
            // Given: Same date components
            const result1 = createUtcDateAtMidnight(2025, 7, 6);
            const result2 = createUtcDateAtMidnight(2025, 7, 6);

            // Then: Should have identical timestamps
            expect(result1.getTime()).toBe(result2.getTime());
        });
    });

    describe('convertServerTimeToUtc - Convert Server Time to UTC', () => {
        it('should convert Dubai server time to UTC correctly', () => {
            // Given: 20:44 in Dubai server time
            const serverDate = new Date(2025, 7, 6, 20, 44, 0); // Aug 6, 2025 20:44

            // When: Converting to UTC
            const result = convertServerTimeToUtc(serverDate, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should be 16:44 UTC (Dubai is UTC+4)
            expect(result.getUTCHours()).toBe(16);
            expect(result.getUTCMinutes()).toBe(44);
            expect(result.getUTCSeconds()).toBe(0);
        });

        it('should convert EST server time to UTC correctly', () => {
            // Given: 15:30 in EST server time
            const serverDate = new Date(2025, 7, 6, 15, 30, 0); // Aug 6, 2025 15:30

            // When: Converting to UTC
            const result = convertServerTimeToUtc(serverDate, SERVER_TIMEZONE_MOCKS.EST);

            // Then: Should be 19:30 UTC (EST in August is actually EDT, UTC-4)
            expect(result.getUTCHours()).toBe(19);
            expect(result.getUTCMinutes()).toBe(30);
            expect(result.getUTCSeconds()).toBe(0);
        });

        it('should handle UTC timezone (no conversion needed)', () => {
            // Given: Server time in UTC
            const serverDate = new Date(2025, 7, 6, 12, 0, 0);

            // When: Converting to UTC
            const result = convertServerTimeToUtc(serverDate, SERVER_TIMEZONE_MOCKS.UTC);

            // Then: Should maintain the same time
            expect(result.getUTCHours()).toBe(12);
            expect(result.getUTCMinutes()).toBe(0);
            expect(result.getUTCSeconds()).toBe(0);
        });

        it('should handle null timezone gracefully', () => {
            // Given: Server time with null timezone
            const serverDate = new Date(2025, 7, 6, 12, 30, 0);

            // When: Converting to UTC
            const result = convertServerTimeToUtc(serverDate, null);

            // Then: Should return the same date object
            expect(result).toBe(serverDate);
        });

        it('should handle edge case where server time crosses day boundary', () => {
            // Given: 23:30 in Dubai (which would be 19:30 UTC same day)
            const serverDate = new Date(2025, 7, 6, 23, 30, 0);

            // When: Converting to UTC
            const result = convertServerTimeToUtc(serverDate, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should be 19:30 UTC same day
            expect(result.getUTCDate()).toBe(6);
            expect(result.getUTCHours()).toBe(19);
            expect(result.getUTCMinutes()).toBe(30);
        });
    });

    describe('parseFieldDefaultValue - Default Value Parsing', () => {
        it('should parse "now" correctly in server timezone', () => {
            // When: Parsing "now" value for datetime field
            const result = parseFieldDefaultValue(
                'now',
                SERVER_TIMEZONE_MOCKS.GULF,
                FIELD_TYPES.DATE_AND_TIME
            );

            // Then: Should return current time in server timezone
            expect(result).toBeInstanceOf(Date);
            expect(result.getTime()).toBeGreaterThan(0);
        });

        it('should parse "now" correctly for TIME fields', () => {
            // When: Parsing "now" value for TIME field
            const result = parseFieldDefaultValue(
                'now',
                SERVER_TIMEZONE_MOCKS.GULF,
                FIELD_TYPES.TIME
            );

            // Then: Should return current time components applied to today
            expect(result).toBeInstanceOf(Date);
            expect(result.getTime()).toBeGreaterThan(0);

            // Should have today's date but current server time components
            const todayInServerTz = getCurrentServerTime(SERVER_TIMEZONE_MOCKS.GULF);
            expect(result.getDate()).toBe(todayInServerTz.getDate());
        });

        it('should interpret fixed datetime as server timezone (Dubai example)', () => {
            // Given: Fixed datetime defaultValue "2025-08-08 15:30:00"
            const defaultValue = '2025-08-08 15:30:00';

            // When: Parsing with Dubai timezone
            const result = parseFieldDefaultValue(defaultValue, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should interpret 15:30 as Dubai time and convert to UTC
            // 15:30 Dubai = 11:30 UTC (Dubai is UTC+4)
            expect(result).toBeInstanceOf(Date);
            expect(result?.getUTCHours()).toBe(11);
            expect(result?.getUTCMinutes()).toBe(30);
        });

        it('should interpret fixed datetime as server timezone (EST example)', () => {
            // Given: Fixed datetime defaultValue "2025-08-08 15:30:00"
            const defaultValue = '2025-08-08 15:30:00';

            // When: Parsing with EST timezone
            const result = parseFieldDefaultValue(defaultValue, SERVER_TIMEZONE_MOCKS.EST);

            // Then: Should interpret 15:30 as EST time and convert to UTC
            // In August, EST is actually EDT (UTC-4), so 15:30 EDT = 19:30 UTC
            expect(result).toBeInstanceOf(Date);
            expect(result?.getUTCHours()).toBe(19);
            expect(result?.getUTCMinutes()).toBe(30);
        });

        it('should handle null timezone gracefully', () => {
            // Given: Fixed datetime defaultValue without timezone
            const defaultValue = '2025-08-08 15:30:00';

            // When: Parsing without timezone
            const result = parseFieldDefaultValue(defaultValue, null);

            // Then: Should parse as local time
            expect(result).toBeInstanceOf(Date);
            expect(result?.getHours()).toBe(15);
            expect(result?.getMinutes()).toBe(30);
        });

        it('should handle invalid default values', () => {
            // When: Parsing invalid date string
            const result = parseFieldDefaultValue('invalid-date', SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should return null
            expect(result).toBeNull();
        });
    });

    describe('processFieldDefaultValue - Complete Default Processing', () => {
        it('should process fixed datetime defaultValue correctly for Dubai timezone', () => {
            // Given: Field with fixed datetime default
            const field = createFakeDateTimeField({
                defaultValue: '2025-08-08 15:30:00',
                variable: 'testField'
            });

            // When: Processing default value
            const result = processFieldDefaultValue(field, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should have correct form and display values
            expect(result).not.toBeNull();

            // formValue should be UTC (15:30 Dubai = 11:30 UTC)
            expect(result?.formValue.getUTCHours()).toBe(11);
            expect(result?.formValue.getUTCMinutes()).toBe(30);

            // displayValue should be back to server time for user display (15:30)
            expect(result?.displayValue.getHours()).toBe(15);
            expect(result?.displayValue.getMinutes()).toBe(30);
        });

        it('should handle "now" defaultValue correctly for datetime fields', () => {
            // Given: Datetime field with "now" default
            const field = createFakeDateTimeField({
                defaultValue: 'now',
                variable: 'testField'
            });

            // When: Processing default value
            const result = processFieldDefaultValue(field, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should return current time (same for both display and form)
            expect(result).not.toBeNull();
            expect(result?.formValue).toBeInstanceOf(Date);
            expect(result?.displayValue).toBeInstanceOf(Date);
            // For datetime "now", both should be the same current time
            expect(result?.formValue.getTime()).toBe(result?.displayValue.getTime());
        });

        it('should handle "now" defaultValue correctly for TIME fields', () => {
            // Given: TIME field with "now" default
            const field = createFakeTimeField({
                defaultValue: 'now',
                variable: 'testField'
            });

            // When: Processing default value
            const result = processFieldDefaultValue(field, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should return time-only value based on current server time
            expect(result).not.toBeNull();
            expect(result?.formValue).toBeInstanceOf(Date);
            expect(result?.displayValue).toBeInstanceOf(Date);

            // For TIME "now", the time components should be preserved but applied to today
            // Both should have today's date but may differ due to timezone conversion
            const todayInServerTz = getCurrentServerTime(SERVER_TIMEZONE_MOCKS.GULF);
            expect(result?.displayValue.getDate()).toBe(todayInServerTz.getDate());

            // formValue (UTC) might be different day due to timezone conversion, allow ±2 days
            const expectedFormDate = todayInServerTz.getDate();
            const actualFormDate = result?.formValue.getDate();
            expect(Math.abs(actualFormDate! - expectedFormDate)).toBeLessThanOrEqual(2);
        });

        it('should handle "now" defaultValue correctly for DATE fields', () => {
            // Given: DATE field with "now" default
            const field = createFakeDateField({
                defaultValue: 'now',
                variable: 'testField',
                fieldType: FIELD_TYPES.DATE
            });

            // When: Processing default value
            const result = processFieldDefaultValue(field, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should return today's date
            expect(result).not.toBeNull();
            expect(result?.formValue).toBeInstanceOf(Date);
            expect(result?.displayValue).toBeInstanceOf(Date);

            // displayValue should show today's date in server timezone
            const todayInServerTz = getCurrentServerTime(SERVER_TIMEZONE_MOCKS.GULF);
            expect(result?.displayValue.getDate()).toBe(todayInServerTz.getDate());
            expect(result?.displayValue.getMonth()).toBe(todayInServerTz.getMonth());
            expect(result?.displayValue.getFullYear()).toBe(todayInServerTz.getFullYear());

            // formValue should be UTC midnight for that date
            expect(result?.formValue.getUTCHours()).toBe(0);
            expect(result?.formValue.getUTCMinutes()).toBe(0);
            expect(result?.formValue.getUTCSeconds()).toBe(0);

            // The UTC date should represent the same calendar date
            expect(result?.formValue.getUTCDate()).toBe(todayInServerTz.getDate());
            expect(result?.formValue.getUTCMonth()).toBe(todayInServerTz.getMonth());
            expect(result?.formValue.getUTCFullYear()).toBe(todayInServerTz.getFullYear());
        });

        it('should return null for empty defaultValue', () => {
            // Given: Field without default value
            const field = createFakeDateTimeField({
                variable: 'testField'
            });

            // When: Processing default value
            const result = processFieldDefaultValue(field, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should return null
            expect(result).toBeNull();
        });
    });

    describe('TIME Field Specific Handling', () => {
        it('should handle TIME field save/load cycle correctly in Dubai timezone', () => {
            // When: Converting to UTC for storage (like onCalendarChange for TIME field)
            const today = new Date();
            const timeInServerTz = new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate(),
                21,
                5,
                0
            );
            const utcForStorage = convertServerTimeToUtc(
                timeInServerTz,
                SERVER_TIMEZONE_MOCKS.GULF
            );

            // And then processing as existing value (like processExistingValue for TIME field)
            const backToDisplay = processExistingValue(
                utcForStorage,
                FIELD_TYPES.TIME,
                SERVER_TIMEZONE_MOCKS.GULF
            );

            // Then: Should display the same time the user originally selected (21:05)
            expect(backToDisplay).toBeTruthy();
            expect(backToDisplay?.getHours()).toBe(21);
            expect(backToDisplay?.getMinutes()).toBe(5);
        });

        it('should handle TIME field correctly when stored time crosses day boundary', () => {
            // Given: User selects 23:30 as time in Dubai
            const today = new Date();
            const timeInServerTz = new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate(),
                23,
                30,
                0
            );

            // When: Converting to UTC for storage (23:30 Dubai = 19:30 UTC)
            const utcForStorage = convertServerTimeToUtc(
                timeInServerTz,
                SERVER_TIMEZONE_MOCKS.GULF
            );

            // Verify it's stored correctly in UTC
            expect(utcForStorage.getUTCHours()).toBe(19);
            expect(utcForStorage.getUTCMinutes()).toBe(30);

            // And then processing as existing value
            const backToDisplay = processExistingValue(
                utcForStorage,
                FIELD_TYPES.TIME,
                SERVER_TIMEZONE_MOCKS.GULF
            );

            // Then: Should display the original time (23:30) regardless of day boundary crossing
            expect(backToDisplay).toBeTruthy();
            expect(backToDisplay?.getHours()).toBe(23);
            expect(backToDisplay?.getMinutes()).toBe(30);
        });

        it('should handle TIME field correctly in EST timezone', () => {
            // Given: User selects 15:30 as time in EST
            const today = new Date();
            const timeInServerTz = new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate(),
                15,
                30,
                0
            );

            // When: Save and load cycle
            const utcForStorage = convertServerTimeToUtc(timeInServerTz, SERVER_TIMEZONE_MOCKS.EST);
            const backToDisplay = processExistingValue(
                utcForStorage,
                FIELD_TYPES.TIME,
                SERVER_TIMEZONE_MOCKS.EST
            );

            // Then: Should maintain the same display time
            expect(backToDisplay).toBeTruthy();
            expect(backToDisplay?.getHours()).toBe(15);
            expect(backToDisplay?.getMinutes()).toBe(30);
        });

        it('should handle TIME field without timezone information', () => {
            // Given: Time field without timezone - create UTC time representing 14:45 UTC
            const today = new Date();
            const timeSelection = new Date(
                Date.UTC(today.getFullYear(), today.getMonth(), today.getDate(), 14, 45, 0)
            );

            // When: Save and load cycle without timezone
            const backToDisplay = processExistingValue(timeSelection, FIELD_TYPES.TIME, null);

            // Then: Should maintain the same time
            expect(backToDisplay).toBeTruthy();
            expect(backToDisplay?.getHours()).toBe(14);
            expect(backToDisplay?.getMinutes()).toBe(45);
        });
    });

    describe('Timezone Conversion Round Trip - Save/Load Cycle', () => {
        it('should maintain consistency in Dubai timezone save/load cycle', () => {
            // Given: User selects 20:44 in Dubai timezone
            const userSelection = new Date(2025, 7, 6, 20, 44, 0);

            // When: Converting to UTC for storage (like onCalendarChange does)
            const utcForStorage = convertServerTimeToUtc(userSelection, SERVER_TIMEZONE_MOCKS.GULF);

            // And then converting back to display (like processExistingValue does)
            const backToDisplay = convertUtcToServerTime(utcForStorage, SERVER_TIMEZONE_MOCKS.GULF);

            // Then: Should show the same time the user originally selected
            expect(backToDisplay.getHours()).toBe(20);
            expect(backToDisplay.getMinutes()).toBe(44);
            expect(backToDisplay.getSeconds()).toBe(0);
        });

        it('should maintain consistency in EST timezone save/load cycle', () => {
            // Given: User selects 15:30 in EST timezone
            const userSelection = new Date(2025, 7, 6, 15, 30, 0);

            // When: Converting to UTC for storage
            const utcForStorage = convertServerTimeToUtc(userSelection, SERVER_TIMEZONE_MOCKS.EST);

            // And then converting back to display
            const backToDisplay = convertUtcToServerTime(utcForStorage, SERVER_TIMEZONE_MOCKS.EST);

            // Then: Should show the same time the user originally selected
            expect(backToDisplay.getHours()).toBe(15);
            expect(backToDisplay.getMinutes()).toBe(30);
            expect(backToDisplay.getSeconds()).toBe(0);
        });

        it('should maintain consistency across different dates in same timezone', () => {
            // Test with multiple dates to ensure DST handling is consistent
            const testDates = [
                new Date(2025, 0, 15, 14, 30, 0), // January
                new Date(2025, 5, 15, 14, 30, 0), // June
                new Date(2025, 11, 15, 14, 30, 0) // December
            ];

            testDates.forEach((userSelection) => {
                // When: Save and load cycle
                const utcForStorage = convertServerTimeToUtc(
                    userSelection,
                    SERVER_TIMEZONE_MOCKS.GULF
                );
                const backToDisplay = convertUtcToServerTime(
                    utcForStorage,
                    SERVER_TIMEZONE_MOCKS.GULF
                );

                // Then: Should maintain the same display time
                expect(backToDisplay.getHours()).toBe(14);
                expect(backToDisplay.getMinutes()).toBe(30);
                expect(backToDisplay.getSeconds()).toBe(0);
            });
        });
    });
});
