import { Pipe, PipeTransform } from '@angular/core';

import { DotFormatDateService } from '@dotcms/data-access';

/*
 * Custom Pipe that returns the relative date.
 */
@Pipe({ name: 'dotRelativeDate', standalone: true })
export class DotRelativeDatePipe implements PipeTransform {
    constructor(private dotFormatDateService: DotFormatDateService) {}

    transform(
        time: string | number = new Date().getTime(),
        format: string = 'MM/dd/yyyy',
        daysLimit: number = 7
    ): string {
        const isMilliseconds = !isNaN(Number(time));

        // Sometimes the time is a string with this format 2/8/2023 - 10:08 PM
        // We need to get rid of that dash
        const cleanDate = isMilliseconds
            ? new Date(Number(time))
            : new Date((time as string).replace('- ', ''));

        // This is tricky because if it comes in miliseconds the transform will be to System Timezone
        // We need it in UTC
        // When the date is a timestamp date the conversion will be done as it is and not transformed to System Timezone
        // So we don't need to convert it to UTC, because it is already in UTC format (the backend works with UTC dates)
        const finalDate = isMilliseconds ? this.dotFormatDateService.getUTC(cleanDate) : cleanDate;

        // Check how many days are between the final date and the current date
        const showTimeStamp =
            Math.abs(
                this.dotFormatDateService.differenceInCalendarDays(
                    finalDate,
                    this.dotFormatDateService.getUTC()
                )
            ) > daysLimit;

        return showTimeStamp
            ? this.dotFormatDateService.format(finalDate, format)
            : this.dotFormatDateService.getRelative(finalDate);
    }
}
