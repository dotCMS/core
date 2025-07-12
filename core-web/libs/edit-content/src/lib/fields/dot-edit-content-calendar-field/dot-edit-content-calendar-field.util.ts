import { SystemTimezone } from '@dotcms/dotcms-js';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

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
 * Ensures that a value is a valid Date object
 * @param value - Value to convert to Date
 * @returns Date object if conversion is successful, null if no value, or original value if already Date
 */
export function ensureValueIsDate(value: any): Date | null {
    // If already a Date, return as is
    if (value instanceof Date) {
        return value;
    }

    // If no value, return null
    if (!value) {
        return null;
    }

    let convertedDate: Date | null = null;

    if (typeof value === 'number') {
        convertedDate = new Date(value);
    } else if (typeof value === 'string') {
        // Timestamp as string
        if (/^\d+$/.test(value)) {
            convertedDate = new Date(parseInt(value, 10));
        } else {
            convertedDate = new Date(value);
        }
    } else {
        // Try direct conversion
        try {
            convertedDate = new Date(value);
        } catch (error) {
            return null;
        }
    }

    // Verify if conversion was successful
    if (convertedDate && !isNaN(convertedDate.getTime())) {
        return convertedDate;
    }

    return null;
}

/**
 * Converts date from server to correct display format
 * The timestamp represents the server's local time in its timezone.
 * We need to adjust to show that server local time regardless of
 * the browser's timezone.
 *
 * @param date - Date object created from server timestamp
 * @param timezone - System timezone configuration
 * @returns Date adjusted to correctly display server time
 */
export function convertToServerTimezoneForDisplay(date: Date, timezone: SystemTimezone | null): Date {
    if (!timezone) {
        return date;
    }

    // Server offset is in milliseconds
    const serverOffsetMs = Number(timezone.offset);

    console.log('serverOffsetMs', serverOffsetMs);

    // Convert UTC timestamp to server timezone
    // Simply add the server offset to get the server local time
    const serverTimestamp = date.getTime() + serverOffsetMs;

    console.log('serverTimestamp', serverTimestamp);

    // Return the date object representing server local time
    return new Date(serverTimestamp);
}

/**
 * Converts date from server timezone to UTC for saving
 * @param serverDate - Date as displayed in server timezone
 * @param timezone - System timezone configuration
 * @returns Date in UTC to send to backend
 */
export function convertToUTCForSaving(serverDate: Date, timezone: SystemTimezone | null): Date {
    if (!timezone || Number(timezone.offset) === 0) {
        // If UTC or no timezone, return as is
        return serverDate;
    }

    try {
        // For saving: convert from server time to UTC
        const serverOffsetMs = Number(timezone.offset); // Already in milliseconds

        // Subtract offset to get UTC
        const utcTimestamp = serverDate.getTime() - serverOffsetMs;
        return new Date(utcTimestamp);
    } catch (error) {
        return serverDate;
    }
}
