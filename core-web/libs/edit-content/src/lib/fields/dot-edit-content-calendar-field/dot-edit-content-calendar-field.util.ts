import { TZDate } from '@date-fns/tz';

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

    // If the server timezone is UTC, no conversion is needed
    if (systemTimezone.id === 'UTC') {
        return utcDate;
    }

    // Validate input date
    if (isNaN(utcDate.getTime())) {
        console.error('Invalid UTC date provided to convertUtcToServerTime:', utcDate);
        return utcDate;
    }

    try {
        // Convert UTC time to server timezone using @date-fns/tz
        // TZDate constructor creates a date in the specified timezone
        // To mimic toZonedTime behavior, we need to create a local date with the UTC components
        // shifted to appear as if they were in the target timezone
        const tzDate = new TZDate(utcDate, systemTimezone.id);

        // Extract the timezone-aware components and create a local date
        const converted = new Date(
            tzDate.getFullYear(),
            tzDate.getMonth(),
            tzDate.getDate(),
            tzDate.getHours(),
            tzDate.getMinutes(),
            tzDate.getSeconds(),
            tzDate.getMilliseconds()
        );

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
        // Create a TZDate representing the date components in the server timezone
        // This correctly interprets the components as being in the specified timezone
        const tzDate = new TZDate(year, month, date, hours, minutes, seconds, 0, systemTimezone.id);

        // TZDate IS a Date object representing the same UTC moment
        const serverMoment = tzDate;

        if (isNaN(serverMoment.getTime())) {
            console.error('Failed to create server timezone date, resulting in invalid date:', {
                year,
                month,
                date,
                hours,
                minutes,
                seconds,
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
 * Safely converts various timestamp formats to a Date object
 * @param timestamp - Timestamp in various formats (Date, number, string)
 * @returns Valid Date object or Date(NaN) if conversion fails
 */
const safeTimestampToDate = (timestamp: Date | number | string): Date => {
    if (timestamp instanceof Date) {
        return timestamp;
    }

    if (typeof timestamp === 'string') {
        // Handle empty strings
        if (!timestamp || timestamp.trim() === '') {
            console.warn('Empty string timestamp provided to safeTimestampToDate');
            return new Date(NaN);
        }

        // Try to parse as number first (for timestamp strings like "1755110657000")
        const numericValue = Number(timestamp);
        if (!isNaN(numericValue)) {
            const result = new Date(numericValue);
            if (isNaN(result.getTime())) {
                console.error('Failed to create date from numeric string:', {
                    timestamp,
                    numericValue
                });
            }
            return result;
        }

        // If not numeric, try direct Date parsing for ISO strings
        console.warn('Non-numeric string timestamp, attempting ISO parsing:', timestamp);
        const result = new Date(timestamp);
        if (isNaN(result.getTime())) {
            console.error('Failed to parse string timestamp:', timestamp);
        }
        return result;
    }

    // Handle number timestamps
    if (typeof timestamp === 'number') {
        const result = new Date(timestamp);
        if (isNaN(result.getTime())) {
            console.error('Failed to create date from number timestamp:', timestamp);
        }
        return result;
    }

    console.error('Unexpected timestamp type:', typeof timestamp, timestamp);
    return new Date(NaN);
};

/**
 * Converts UTC timestamp to display value for date-only fields
 * Ensures date shows correctly regardless of user's timezone
 * @param utcTimestamp - UTC timestamp from backend (Date object or number)
 * @returns Local date with UTC components for correct display
 */
export const convertUtcTimestampToDateDisplay = (utcTimestamp: Date | number | string): Date => {
    // Ensure we have a Date object - handle string timestamps safely
    const utcDate = safeTimestampToDate(utcTimestamp);

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
 * Gets the current time in server timezone for default calendar navigation (when user clicks)
 * This shows the current time as it appears on the server
 * @param systemTimezone - Server timezone configuration
 * @returns Current date/time in server timezone for calendar display
 */
export const getCurrentServerTime = (systemTimezone: DotSystemTimezone | null): Date => {
    const now = new Date();

    // For UTC server or no timezone, show UTC time components as local time
    if (!systemTimezone?.id || systemTimezone.id === 'UTC') {
        // Create a date showing UTC components as if they were local
        // This shows 18:26 UTC as 18:26 in the calendar
        return new Date(
            now.getUTCFullYear(),
            now.getUTCMonth(),
            now.getUTCDate(),
            now.getUTCHours(),
            now.getUTCMinutes(),
            now.getUTCSeconds()
        );
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
 * Gets the current time for field defaults with "now"
 * This represents the current moment as it should be stored/displayed (UTC for server)
 * @param systemTimezone - Server timezone configuration
 * @returns Current time in server timezone context
 */
export const getCurrentFieldDefaultTime = (systemTimezone: DotSystemTimezone | null): Date => {
    const now = new Date();

    // If server is configured as UTC or no timezone, return current UTC time
    if (!systemTimezone?.id || systemTimezone.id === 'UTC') {
        // For UTC server, return the current UTC time
        return now;
    }

    // For non-UTC server timezones, convert current local time to server time
    try {
        // Get current time in server timezone for display
        return convertUtcToServerTime(now, systemTimezone);
    } catch (error) {
        console.error('Error getting field default time:', error);
        return now;
    }
};

/**
 * Parses the field's defaultValue and returns the appropriate Date or null
 * This handles field-level defaults which are considered to be in server timezone context:
 * - "now": current server time
 * - fixed dates: interpreted as server timezone values, converted to UTC for storage
 * @param defaultValue - The defaultValue from the field configuration
 * @param systemTimezone - Server timezone configuration
 * @param fieldType - The field type to handle TIME fields with "now" specially
 * @returns Date object in UTC for storage, or null if no default
 */
export const parseFieldDefaultValue = (
    defaultValue: string | undefined,
    systemTimezone: DotSystemTimezone | null,
    fieldType?: FieldType
): Date | null => {
    if (!defaultValue) {
        return null;
    }

    // Handle "now" - current time for field defaults (already in server timezone context)
    if (defaultValue.toLowerCase() === 'now') {
        const currentTime = getCurrentFieldDefaultTime(systemTimezone);
        // Validate that we got a valid date
        if (!currentTime || isNaN(currentTime.getTime())) {
            console.error('Failed to get current field default time');
            return null;
        }

        // For TIME fields with "now", return the current time directly
        // The caller will handle proper display/form value separation
        if (fieldType === FIELD_TYPES.TIME) {
            return currentTime;
        }

        return currentTime;
    }

    // Handle fixed date - assume format: "year-month-day hour:minute:second"
    // or other ISO-like formats that can be parsed by Date constructor
    try {
        // For fixed dates in defaultValue, interpret them as server timezone values
        // that need to be converted to UTC for storage
        // Example: "2025-08-08 15:30:00" in Dubai timezone = "2025-08-08 11:30:00" UTC

        let parsedDate: Date;

        // If the defaultValue doesn't include timezone info, treat it as server timezone value
        if (
            !defaultValue.includes('T') ||
            (!defaultValue.includes('Z') && !defaultValue.includes('+'))
        ) {
            // Extract the literal components from the input string
            const dateMatch = defaultValue.match(
                /(\d{4})-(\d{2})-(\d{2})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?/
            );

            if (dateMatch) {
                const [, year, month, day, hours = '0', minutes = '0', seconds = '0'] = dateMatch;

                // Create date representing these components
                if (systemTimezone?.id && systemTimezone.id !== 'UTC') {
                    // For non-UTC server timezones: interpret components as server timezone, convert to UTC for storage
                    parsedDate = createServerTimezoneDate(
                        parseInt(year),
                        parseInt(month) - 1, // month is 0-based
                        parseInt(day),
                        parseInt(hours),
                        parseInt(minutes),
                        parseInt(seconds),
                        systemTimezone
                    );
                } else {
                    // For UTC server: create as literal local time (will be displayed as-is)
                    // This means "2025-08-08 15:30:00" displays as "15:30" exactly
                    parsedDate = new Date(
                        parseInt(year),
                        parseInt(month) - 1,
                        parseInt(day),
                        parseInt(hours),
                        parseInt(minutes),
                        parseInt(seconds)
                    );
                }
            } else {
                // Fallback if regex doesn't match
                parsedDate = new Date(defaultValue);
            }
        } else {
            // For ISO dates with timezone info, use direct parsing
            parsedDate = new Date(defaultValue);
        }

        // Check if the date is valid
        if (isNaN(parsedDate.getTime())) {
            console.warn(`Invalid defaultValue format: ${defaultValue}`);
            return null;
        }

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

    // Parse UTC value to Date object using safe conversion
    const utcDate = safeTimestampToDate(utcValue);

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
        if (!systemTimezone?.id || systemTimezone.id === 'UTC') {
            // For UTC server, show UTC time components as local time for correct display
            return new Date(
                timeOnlyUtc.getUTCFullYear(),
                timeOnlyUtc.getUTCMonth(),
                timeOnlyUtc.getUTCDate(),
                timeOnlyUtc.getUTCHours(),
                timeOnlyUtc.getUTCMinutes(),
                timeOnlyUtc.getUTCSeconds()
            );
        } else {
            // For non-UTC servers, convert UTC to server timezone
            const displayValue = convertUtcToServerTime(timeOnlyUtc, systemTimezone);
            return displayValue;
        }
    }

    // For datetime fields, convert UTC timestamp to server timezone for display
    if (!systemTimezone?.id || systemTimezone.id === 'UTC') {
        // For UTC server, show UTC components as local time for correct display
        // This shows 15:30 UTC as 15:30 in the calendar (not converted to local timezone)
        return new Date(
            utcDate.getUTCFullYear(),
            utcDate.getUTCMonth(),
            utcDate.getUTCDate(),
            utcDate.getUTCHours(),
            utcDate.getUTCMinutes(),
            utcDate.getUTCSeconds()
        );
    } else {
        // For non-UTC servers, convert UTC to server timezone
        const displayValue = convertUtcToServerTime(utcDate, systemTimezone);
        return displayValue;
    }
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
        // For TIME fields with "now": show current server time (UTC components for UTC server)
        if (!systemTimezone?.id || systemTimezone.id === 'UTC') {
            // For UTC server, create time showing UTC components
            const utcNow = new Date();
            displayValue = new Date(
                utcNow.getUTCFullYear(),
                utcNow.getUTCMonth(),
                utcNow.getUTCDate(),
                utcNow.getUTCHours(),
                utcNow.getUTCMinutes(),
                utcNow.getUTCSeconds()
            );
        } else {
            // For non-UTC servers, show the server time
            displayValue = defaultValue;
        }

        // For form storage: extract time components and apply to today's date, then convert to UTC
        const timeComponents = extractDateComponents(displayValue);
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
    } else {
        // For all other cases (fixed datetime/date values, datetime with "now", etc.)
        // Use the parsed value directly without timezone conversion for fixed values
        displayValue = defaultValue;
        formValue = defaultValue;
    }

    return {
        displayValue: displayValue,
        formValue: formValue
    };
};
