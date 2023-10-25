import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

export interface DateOptions {
    showTime?: boolean;
    timeOnly?: boolean;
}

export type CalendarTypes = FIELD_TYPES.DATE_AND_TIME | FIELD_TYPES.DATE | FIELD_TYPES.TIME;

// Object to hold the options of the calendar component per field type
export const CALENDAR_OPTIONS_PER_TYPE: Record<CalendarTypes, DateOptions> = {
    [FIELD_TYPES.DATE_AND_TIME]: {
        showTime: true,
        timeOnly: false
    },
    [FIELD_TYPES.DATE]: {
        showTime: false,
        timeOnly: false
    },
    [FIELD_TYPES.TIME]: {
        showTime: true,
        timeOnly: true
    }
};
