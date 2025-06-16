import { CalendarFieldTypes, DotCMSFieldTypes } from '@dotcms/dotcms-models';

export interface DateOptions {
    showTime?: boolean;
    timeOnly?: boolean;
    icon?: string;
}

export const CALENDAR_OPTIONS_PER_TYPE: Record<CalendarFieldTypes, DateOptions> = {
    [DotCMSFieldTypes.DATE_AND_TIME]: {
        showTime: true,
        timeOnly: false,
        icon: 'pi pi-calendar'
    },
    [DotCMSFieldTypes.DATE]: {
        showTime: false,
        timeOnly: false,
        icon: 'pi pi-calendar'
    },
    [DotCMSFieldTypes.TIME]: {
        showTime: true,
        timeOnly: true,
        icon: 'pi pi-clock'
    }
};
