import { Pipe, PipeTransform } from '@angular/core';

import {
    AnalyticsCountFormatMode,
    formatAnalyticsCount
} from '../../utils/format-analytics-count.util';

@Pipe({
    name: 'dotAnalyticsCount'
})
export class DotAnalyticsCountPipe implements PipeTransform {
    transform(
        value: number | null | undefined,
        mode: AnalyticsCountFormatMode = 'compact'
    ): string {
        if (value == null) {
            return '0';
        }

        return formatAnalyticsCount(value, mode);
    }
}
