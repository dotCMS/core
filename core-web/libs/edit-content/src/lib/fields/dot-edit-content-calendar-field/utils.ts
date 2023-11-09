import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

export interface DateOptions {
    showTime?: boolean;
    timeOnly?: boolean;
    icon?: string;
}

export type CalendarTypes = FIELD_TYPES.DATE_AND_TIME | FIELD_TYPES.DATE | FIELD_TYPES.TIME;

// Object to hold the options of the calendar component per field type
export const CALENDAR_OPTIONS_PER_TYPE: Record<CalendarTypes, DateOptions> = {
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
