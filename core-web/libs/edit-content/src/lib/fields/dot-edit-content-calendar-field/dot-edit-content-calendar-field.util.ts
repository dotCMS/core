import { fromZonedTime, toZonedTime } from 'date-fns-tz';

import { DotCMSContentTypeField, DotSystemTimezone } from '@dotcms/dotcms-models';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FieldType } from '../../models/dot-edit-content-field.type';

export interface DateOptions {
    showTime: boolean;
    timeOnly: boolean;
    icon: string;
}

export type CalendarTypes = FIELD_TYPES.DATE_AND_TIME | FIELD_TYPES.DATE | FIELD_TYPES.TIME;

export type CalendarConfig = Record<CalendarTypes, DateOptions>;

// Object to hold the options of the calendar component per field type
export const CALENDAR_OPTIONS_PER_TYPE: CalendarConfig = {
    [FIELD_TYPES.DATE_AND_TIME]: {
        showTime: true,
        timeOnly: false,
        icon: 'pi pi-calendar'
    },
    [FIELD_TYPES.DATE]: {
        showTime: false,
        timeOnly: false,
        icon: 'pi pi-calendar'
    },
    [FIELD_TYPES.TIME]: {
        showTime: true,
        timeOnly: true,
        icon: 'pi pi-clock'
    }
};

/**
 * Converts UTC date to server timezone for display in the calendar
 * @param utcDate - UTC date from the server
 * @param systemTimezone - Server timezone configuration
 * @returns Date object representing the time in server timezone
 */
export const convertUtcToServerTime = (
    utcDate: Date,
    systemTimezone: DotSystemTimezone | null
): Date => {
    if (!systemTimezone?.id || !utcDate) {
        return utcDate;
    }

    // Validate input date
    if (isNaN(utcDate.getTime())) {
        console.error('Invalid UTC date provided to convertUtcToServerTime:', utcDate);
        return utcDate;
    }

    try {
        // Convert UTC time to server timezone using date-fns-tz
        const converted = toZonedTime(utcDate, systemTimezone.id);

        // Validate the result
        if (!converted || isNaN(converted.getTime())) {
            console.error('Failed to convert UTC to server time, result is invalid:', {
                utcDate,
                timezoneId: systemTimezone.id,
                result: converted
            });
            return utcDate;
        }

        return converted;
    } catch (error) {
        console.error('Error converting UTC to server time:', error, {
            utcDate,
            timezoneId: systemTimezone.id
        });
        return utcDate;
    }
};

/**
 * Creates a date that represents the given components AS IF they were in server timezone
 * @param year - Year component
 * @param month - Month component (0-11)
 * @param date - Date component
 * @param hours - Hours component
 * @param minutes - Minutes component
 * @param seconds - Seconds component
 * @param systemTimezone - Server timezone configuration
 * @returns Date object that represents the time in server timezone (JavaScript Date in local representation)
 */
export const createServerTimezoneDate = (
    year: number,
    month: number,
    date: number,
    hours: number,
    minutes: number,
    seconds: number,
    systemTimezone: DotSystemTimezone | null
): Date => {
    // Validate input parameters
    if (
        isNaN(year) ||
        isNaN(month) ||
        isNaN(date) ||
        isNaN(hours) ||
        isNaN(minutes) ||
        isNaN(seconds)
    ) {
        console.error('Invalid date components provided to createServerTimezoneDate:', {
            year,
            month,
            date,
            hours,
            minutes,
            seconds
        });
        return new Date(NaN); // Return invalid date
    }

    if (!systemTimezone?.id) {
        // If no timezone, create as local time
        const localDate = new Date(year, month, date, hours, minutes, seconds);
        if (isNaN(localDate.getTime())) {
            console.error('Failed to create local date with components:', {
                year,
                month,
                date,
                hours,
                minutes,
                seconds
            });
        }
        return localDate;
    }

    try {
        // Create a date string representing the time in the server timezone
        const isoString = `${year}-${String(month + 1).padStart(2, '0')}-${String(date).padStart(2, '0')}T${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

        // This interprets the date as if it were in the server timezone
        // and returns a JavaScript Date that represents that exact moment in time
        const serverMoment = fromZonedTime(isoString, systemTimezone.id);

        if (isNaN(serverMoment.getTime())) {
            console.error('Failed to create server timezone date, resulting in invalid date:', {
                isoString,
                timezoneId: systemTimezone.id
            });
        }

        return serverMoment;
    } catch (error) {
        console.error('Error creating server timezone date:', error, {
            year,
            month,
            date,
            hours,
            minutes,
            seconds,
            timezoneId: systemTimezone.id
        });
        // Fallback to local timezone
        const fallbackDate = new Date(year, month, date, hours, minutes, seconds);
        if (isNaN(fallbackDate.getTime())) {
            console.error('Fallback date creation also failed');
        }
        return fallbackDate;
    }
};

/**
 * Converts UTC timestamp to display value for date-only fields
 * Ensures date shows correctly regardless of user's timezone
 * @param utcTimestamp - UTC timestamp from backend (Date object or number)
 * @returns Local date with UTC components for correct display
 */
export const convertUtcTimestampToDateDisplay = (utcTimestamp: Date | number | string): Date => {
    // Ensure we have a Date object - handle string timestamps
    let utcDate: Date;
    if (utcTimestamp instanceof Date) {
        utcDate = utcTimestamp;
    } else if (typeof utcTimestamp === 'string') {
        // Convert string timestamp to number first
        utcDate = new Date(Number(utcTimestamp));
    } else {
        utcDate = new Date(utcTimestamp);
    }

    // Validate the date before proceeding
    if (isNaN(utcDate.getTime())) {
        console.error(
            'Invalid UTC timestamp provided to convertUtcTimestampToDateDisplay:',
            utcTimestamp
        );
        return new Date(NaN); // Return invalid date to maintain consistency
    }

    const year = utcDate.getUTCFullYear();
    const month = utcDate.getUTCMonth();
    const date = utcDate.getUTCDate();

    // Create local date with UTC components to show correct date
    const displayDate = new Date(year, month, date);

    // Validate the result
    if (isNaN(displayDate.getTime())) {
        console.error('Failed to create display date from UTC components:', {
            year,
            month,
            date,
            originalTimestamp: utcTimestamp
        });
    }

    return displayDate;
};

/**
 * Extracts date/time components from a Date object
 * @param date - Date object to extract components from
 * @returns Object with individual date/time components
 */
export const extractDateComponents = (date: Date) => ({
    year: date.getFullYear(),
    month: date.getMonth(),
    date: date.getDate(),
    hours: date.getHours(),
    minutes: date.getMinutes(),
    seconds: date.getSeconds()
});

/**
 * Creates UTC date at midnight from date components
 * Used for date-only fields to represent "the date" globally
 * @param year - Year component
 * @param month - Month component (0-11)
 * @param date - Date component
 * @returns UTC date at midnight
 */
export const createUtcDateAtMidnight = (year: number, month: number, date: number): Date => {
    return new Date(Date.UTC(year, month, date, 0, 0, 0));
};

/**
 * Gets the current time in server timezone for default calendar navigation
 * @param systemTimezone - Server timezone configuration
 * @returns Current date/time in server timezone
 */
export const getCurrentServerTime = (systemTimezone: DotSystemTimezone | null): Date => {
    const now = new Date();

    if (!systemTimezone?.id) {
        return now;
    }

    try {
        const serverTime = convertUtcToServerTime(now, systemTimezone);

        // Validate the result
        if (!serverTime || isNaN(serverTime.getTime())) {
            console.error(
                'Failed to convert current time to server timezone, falling back to local time'
            );
            return now;
        }

        return serverTime;
    } catch (error) {
        console.error('Error getting current server time, falling back to local time:', error);
        return now;
    }
};

/**
 * Parses the field's defaultValue and returns the appropriate Date or null
 * @param defaultValue - The defaultValue from the field configuration
 * @param systemTimezone - Server timezone configuration
 * @param fieldType - The field type to handle TIME fields with "now" specially
 * @returns Date object for default value or null if no default
 */
export const parseFieldDefaultValue = (
    defaultValue: string | undefined,
    systemTimezone: DotSystemTimezone | null,
    fieldType?: FieldType
): Date | null => {
    if (!defaultValue) {
        return null;
    }

    // Handle "now" - current server time
    if (defaultValue.toLowerCase() === 'now') {
        const currentTime = getCurrentServerTime(systemTimezone);
        // Validate that we got a valid date
        if (!currentTime || isNaN(currentTime.getTime())) {
            console.error('Failed to get current server time');
            return null;
        }

        // For TIME fields with "now", return the current server time directly
        // The caller will handle proper display/form value separation
        if (fieldType === FIELD_TYPES.TIME) {
            return currentTime;
        }

        return currentTime;
    }

    // Handle fixed date - assume format: "year-month-day hour:minute:second"
    // or other ISO-like formats that can be parsed by Date constructor
    try {
        // Parse the default value as if it's in server timezone
        const parsedDate = new Date(defaultValue);

        // Check if the date is valid
        if (isNaN(parsedDate.getTime())) {
            console.warn(`Invalid defaultValue format: ${defaultValue}`);
            return null;
        }

        // For fixed dates in defaultValue, we need to interpret the components
        // as if they were in server timezone, not local timezone
        if (systemTimezone?.id) {
            // Extract the components from the parsed date (which was interpreted in local time)
            const { year, month, date, hours, minutes, seconds } =
                extractDateComponents(parsedDate);

            // Create a date that represents these components as if they were in server timezone
            return createServerTimezoneDate(
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
                systemTimezone
            );
        }

        // If no timezone info, return as-is
        return parsedDate;
    } catch (error) {
        console.error(`Error parsing defaultValue: ${defaultValue}`, error);
        return null;
    }
};

/**
 * Processes an existing UTC value to display format based on field type
 * @param utcValue - The UTC value from form/backend
 * @param fieldType - The type of calendar field
 * @param systemTimezone - Server timezone configuration
 * @returns Date for display or null if invalid
 */
export const processExistingValue = (
    utcValue: Date | number | string,
    fieldType: FieldType,
    systemTimezone: DotSystemTimezone | null
): Date | null => {
    if (fieldType === 'Date') {
        // For date-only fields, convert UTC timestamp to proper display value
        const displayValue = convertUtcTimestampToDateDisplay(utcValue);
        return isNaN(displayValue.getTime()) ? null : displayValue;
    }

    // Parse UTC value to Date object
    let utcDate: Date;
    if (utcValue instanceof Date) {
        utcDate = utcValue;
    } else if (typeof utcValue === 'string') {
        utcDate = new Date(Number(utcValue));
    } else {
        utcDate = new Date(utcValue);
    }

    // Validate the parsed date
    if (isNaN(utcDate.getTime())) {
        console.error('Invalid value provided:', utcValue);
        return null;
    }

    if (fieldType === 'Time') {
        // For time-only fields, extract time components from UTC and apply to today's date
        // This ensures the time displays correctly regardless of the original date
        const timeComponents = {
            hours: utcDate.getUTCHours(),
            minutes: utcDate.getUTCMinutes(),
            seconds: utcDate.getUTCSeconds()
        };

        // Create a date with today's date but with the stored time components
        const today = new Date();
        const timeOnlyUtc = new Date(
            Date.UTC(
                today.getFullYear(),
                today.getMonth(),
                today.getDate(),
                timeComponents.hours,
                timeComponents.minutes,
                timeComponents.seconds
            )
        );

        // Convert to server timezone for display
        const displayValue = systemTimezone?.id
            ? convertUtcToServerTime(timeOnlyUtc, systemTimezone)
            : timeOnlyUtc;

        return displayValue;
    }

    // For datetime fields, convert UTC timestamp to server timezone for display
    const displayValue = systemTimezone?.id
        ? convertUtcToServerTime(utcDate, systemTimezone)
        : utcDate;

    return displayValue;
};

/**
 * Converts server timezone date to UTC for storing in form control
 * @param serverDate - Date in server timezone
 * @param systemTimezone - Server timezone configuration
 * @returns UTC date representing the same moment
 */
export const convertServerTimeToUtc = (
    serverDate: Date,
    systemTimezone: DotSystemTimezone | null
): Date => {
    if (!systemTimezone?.id || !serverDate) {
        return serverDate;
    }

    // Validate input date
    if (isNaN(serverDate.getTime())) {
        console.error('Invalid server date provided to convertServerTimeToUtc:', serverDate);
        return serverDate;
    }

    try {
        // Extract components from the date (these represent server timezone values)
        const { year, month, date, hours, minutes, seconds } = extractDateComponents(serverDate);

        // Create a date that represents this moment in server timezone
        const serverMoment = createServerTimezoneDate(
            year,
            month,
            date,
            hours,
            minutes,
            seconds,
            systemTimezone
        );

        // Validate the result
        if (!serverMoment || isNaN(serverMoment.getTime())) {
            console.error('Failed to convert server time to UTC, result is invalid:', {
                serverDate,
                timezoneId: systemTimezone.id,
                result: serverMoment
            });
            return serverDate;
        }

        return serverMoment;
    } catch (error) {
        console.error('Error converting server time to UTC:', error, {
            serverDate,
            timezoneId: systemTimezone.id
        });
        return serverDate;
    }
};

/**
 * Processes field default value for new/empty fields
 * @param field - The field configuration with fieldType and defaultValue
 * @param systemTimezone - Server timezone configuration
 * @returns Object with display value and form value, or null if no default
 */
export const processFieldDefaultValue = (
    field: DotCMSContentTypeField,
    systemTimezone: DotSystemTimezone | null
): { displayValue: Date; formValue: Date } | null => {
    if (!field.defaultValue) {
        return null;
    }
    const fieldType = field.fieldType as FieldType;

    const defaultValue = parseFieldDefaultValue(field.defaultValue, systemTimezone, fieldType);

    if (!defaultValue || isNaN(defaultValue.getTime())) {
        console.warn(`Field ${field.variable} has invalid defaultValue: ${field.defaultValue}`);
        return null;
    }

    // Handle display and form values based on field type and default value type
    let displayValue: Date;
    let formValue: Date;

    if (fieldType === 'Time' && field.defaultValue.toLowerCase() === 'now') {
        // For TIME fields with "now": show current server time directly to user
        displayValue = defaultValue;

        // For form storage: extract time components and apply to today's date, then convert to UTC
        const timeComponents = extractDateComponents(defaultValue);
        const today = new Date();
        const timeForForm = new Date(
            today.getFullYear(),
            today.getMonth(),
            today.getDate(),
            timeComponents.hours,
            timeComponents.minutes,
            timeComponents.seconds
        );
        formValue = convertServerTimeToUtc(timeForForm, systemTimezone);
    } else if (fieldType === 'Date' && field.defaultValue.toLowerCase() === 'now') {
        // For DATE fields with "now": show today's date in server timezone
        displayValue = defaultValue;

        // For form storage: create UTC midnight for today's date in server timezone
        const dateComponents = extractDateComponents(defaultValue);
        formValue = createUtcDateAtMidnight(
            dateComponents.year,
            dateComponents.month,
            dateComponents.date
        );
    } else if (
        fieldType !== 'Time' &&
        field.defaultValue.toLowerCase() !== 'now' &&
        systemTimezone?.id
    ) {
        // For datetime/date fields with fixed default values (not "now")
        displayValue = convertUtcToServerTime(defaultValue, systemTimezone);
        formValue = defaultValue; // Already in UTC from parseFieldDefaultValue
    } else {
        // For all other cases (datetime with "now", etc.)
        displayValue = defaultValue;
        formValue = defaultValue;
    }

    return {
        displayValue: displayValue,
        formValue: formValue
    };
};
