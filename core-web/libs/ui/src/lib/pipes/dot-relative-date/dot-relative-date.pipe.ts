import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';

/*
 * Custom Pipe that returns the relative date.
 */
@Pipe({ name: 'dotRelativeDate' })
export class DotRelativeDatePipe implements PipeTransform {
    private readonly dotFormatDateService = inject(DotFormatDateService);
    private readonly dotMessageService = inject(DotMessageService);

    transform(date: string | number, format = 'MM/dd/yyyy', timeStampAfter = 7): string {
        const time = date || new Date().getTime();
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
        const endDate = isMilliseconds ? this.dotFormatDateService.getUTC(cleanDate) : cleanDate;

        // Check how many days are between the final date and the current date
        // Absolute value because we don't care if it is in the past or future
        const diffTime = Math.abs(
            this.dotFormatDateService.differenceInCalendarDays(
                this.dotFormatDateService.getUTC(),
                endDate
            )
        );

        // Check if the timeStampAfter is a valid number
        const validTimeAfter = !isNaN(timeStampAfter) && timeStampAfter !== null;

        // If the diffTime is less than the timeStampAfter we show the relative time
        const showRelativeTime = validTimeAfter ? diffTime < timeStampAfter : true;

        if (diffTime === 0 && showRelativeTime) {
            const nowTimestamp = new Date().getTime();
            const inputTimestamp = new Date(time).getTime();
            const diffInSeconds = Math.abs(nowTimestamp - inputTimestamp) / 1000;

            // Only consider dates in the past or now
            // If it's a future date with the same day, we use the standard format
            if (inputTimestamp > nowTimestamp) {
                return this.dotFormatDateService.getRelative(endDate);
            }

            // Less than 30 seconds
            if (diffInSeconds < 30) {
                return this.dotMessageService.get('relative.date.now');
            }

            // Less than 2 minutes
            if (diffInSeconds < 120) {
                return this.dotMessageService.get('relative.date.minute.ago');
            }

            // Less than 1 hour
            if (diffInSeconds < 3600) {
                const minutes = Math.floor(diffInSeconds / 60);

                return this.dotMessageService.get('relative.date.minutes.ago', minutes.toString());
            }

            // Less than 24 hours
            if (diffInSeconds < 86400) {
                const hours = Math.floor(diffInSeconds / 3600);

                return this.dotMessageService.get('relative.date.hours.ago', hours.toString());
            }
        }

        return showRelativeTime
            ? this.dotFormatDateService.getRelative(endDate)
            : this.dotFormatDateService.format(endDate, format);
    }
}
