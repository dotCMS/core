import { UVE_MODE } from '@dotcms/uve/types';

export interface AnalyticsUVEModeChange {
    toMode: UVE_MODE;
    fromMode: UVE_MODE;
}

export interface AnalyticsUVECalendarChange {
    selectedDate: string;
}
