import { JsonPipe, NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    inject,
    input,
    computed,
    Signal,
    signal,
    OnInit,
    effect
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { CalendarModule } from 'primeng/calendar';

import { SystemTimezone } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { CALENDAR_OPTIONS_PER_TYPE, CalendarConfig, DateOptions } from './utils';

/**
 * DotEditContentCalendarFieldComponent
 *
 * This Angular component provides a reusable form field that can act as:
 * - Calendar Field (date)
 * - Time Field (time)
 * - Datetime Field (date and time)
 *
 * The field type and its behavior are determined by the configuration of the `$field` input
 * and the options defined in `CALENDAR_OPTIONS_PER_TYPE`.
 *
 * ## Inputs
 * - `$field` (DotCMSContentTypeField, required): The DotCMS field configuration that defines the type (calendar, time, datetime) and other settings.
 * - `$systemTimezone` (SystemTimezone | null, alias: 'utcTimezone'): The system timezone, used to adjust the display and storage of date/time values.
 */
@Component({
    selector: 'dot-edit-content-calendar-field',
    standalone: true,
    imports: [CalendarModule, ReactiveFormsModule, JsonPipe, NgIf],
    templateUrl: 'dot-edit-content-calendar-field.component.html',
    styleUrls: ['./dot-edit-content-calendar-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentCalendarFieldComponent {
    /**
     * The default date to display in the calendar.
     * It is computed based on the system timezone.
     */
    readonly $defaultDate = computed(() => {
        const timezone = this.$systemTimezone();
        if (timezone) {
            return this.getServerAdjustedDate();
        }

        return null;
    });

    /**
     * The field configuration (required).
     * Determines the type of calendar field (date, time, datetime).
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * The system timezone (optional).
     * Used to adjust the display and storage of date/time values.
     * Alias: utcTimezone
     */
    $systemTimezone = input<SystemTimezone | null>(null, { alias: 'utcTimezone' });

    /**
     * The configuration for the field type.
     */
    $fieldTypeConfig = computed(() => {
        const fieldType = this.$field().fieldType;

        return CALENDAR_OPTIONS_PER_TYPE[fieldType];
    });

    /**
     * Gets the current date adjusted to server timezone
     * @returns Date object adjusted to server timezone
     */
    private getServerAdjustedDate(): Date {
        const now = new Date();

        // Get current UTC timestamp and create new UTC date
        const utcTimestamp = now.getTime() + now.getTimezoneOffset() * 60 * 1000;

        return new Date(utcTimestamp);
    }
}
