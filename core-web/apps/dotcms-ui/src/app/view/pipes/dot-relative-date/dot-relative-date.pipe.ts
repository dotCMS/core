import { Pipe, PipeTransform } from '@angular/core';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';

/*
 * Custom Pipe that returns the relative date.
 */
@Pipe({ name: 'dotRelativeDate', standalone: true })
export class DotRelativeDatePipe implements PipeTransform {
    constructor(private dotFormatDateService: DotFormatDateService) {}

    transform(value: string, formatedInMilliseconds = false): string {
        return this.dotFormatDateService.getRelative(
            formatedInMilliseconds ? value : new Date(value).getTime().toString(),
            // The backend returns the date in UTC, so we need to convert the system date to UTC date to have the correct time difference
            new Date(new Date().toUTCString().slice(0, -4)) // This deletes the GMT from the date, so it can be parsed correctly
        );
    }
}
