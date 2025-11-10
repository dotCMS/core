import { inject, Pipe, PipeTransform } from '@angular/core';

import { DotFormatDateService } from '@dotcms/data-access';

/**
 * Transforms a timestamp into a formatted date string based on the user's selected language at login
 *
 * @remarks
 * This pipe is a pure pipe, meaning it is only re-evaluated when the input value changes.
 *
 * @example
 * ```html
 * <p>{{ timestampValue | dotTimestampToDate }}</p>
 * ```
 *
 * @param time - The timestamp to be transformed into a date string.
 * @param userDateFormatOptions - Optional. The formatting options for the date string.
 * @returns A formatted date string based on the provided timestamp and formatting options.
 */
@Pipe({
    name: 'dotTimestampToDate',
    pure: true
})
export class DotTimestampToDatePipe implements PipeTransform {
    private dotFormatDateService: DotFormatDateService = inject(DotFormatDateService);
    transform(time: number, userDateFormatOptions?: Intl.DateTimeFormatOptions): string {
        return this.dotFormatDateService.getDateFromTimestamp(time, userDateFormatOptions);
    }
}
